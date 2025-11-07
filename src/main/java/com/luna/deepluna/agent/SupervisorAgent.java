package com.luna.deepluna.agent;

import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.agent.agentTool.SupervisorTools;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisorAgent {

    private final SupervisorTools supervisorTools;

    private final ChatClientCache chatClientCache;

    private final ToolCallingManager toolCallingManager;

    private final ContextCache contextCache;

    public void startResearch(String sessionId, String researchBrief) {
        SupervisorAgentContext supervisorAgentContext = SupervisorAgentContext.builder()
                .supervisorId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .researchBrief(researchBrief)
                .maxSubAgentsNumber(5L)
                .status(SupervisorAgentState.INITIALIZING)
                .notes(new ArrayList<>())
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .build())
                .build();
        contextCache.putSupervisor(supervisorAgentContext.getSessionId(), supervisorAgentContext);
        supervisorAgent(supervisorAgentContext.getSessionId());
    }

    private void supervisorAgent(String sessionId) {
        OpenAiChatModel chatModel = chatClientCache.getChatClient(sessionId);
        AssertUtil.isNotNull(chatModel, "Chat model not found for sessionId: " + sessionId);

        SupervisorAgentContext supervisorAgentContext = contextCache.getSupervisor(sessionId);
        AssertUtil.isNotNull(supervisorAgentContext, "SupervisorAgentContext not found for sessionId: " + sessionId);

        String supervisorId = supervisorAgentContext.getSupervisorId();
        ChatMemory chatMemory = supervisorAgentContext.getChatMemory();

        // 初始化对话记忆,注入初始提示语和研究简报
        chatMemory.add(supervisorId, new AssistantMessage(Prompts.SUPERVISOR_PROMPT.formatted(
                LocalDateTime.now(),
                5,
                supervisorAgentContext.getMaxSubAgentsNumber()
        )));
        chatMemory.add(supervisorId, new UserMessage("Research Brief:" + supervisorAgentContext.getResearchBrief()));

        log.info("Supervisor Agent started: supervisorId={}, sessionId={}",
                supervisorId, supervisorAgentContext.getSessionId());
        // 设置状态为运行中
        supervisorAgentContext.setStatus(SupervisorAgentState.RUNNING);

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(supervisorTools))
                .internalToolExecutionEnabled(false)
                .build();

        Prompt promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);

        ChatResponse response = chatModel.call(promptWithMemory);
        // 循环处理工具调用与思考，完成了会退出
        while (response.hasToolCalls()) {
            Generation result = response.getResult();
            // 记录调用工具信息到对话记忆
            log.info("Supervisor Agent received tool calls: supervisorId={}, sessionId={}, toolCalls={}",
                    supervisorId, supervisorAgentContext.getSessionId(), result.getOutput().getToolCalls());
            chatMemory.add(supervisorId, response.getResult().getOutput());
            List<AssistantMessage.ToolCall> toolCalls = result.getOutput().getToolCalls();
            List<AssistantMessage.ToolCall> researchComplete = toolCalls.stream().filter(toolCall -> toolCall.name().equals("researchComplete")).toList();
            boolean isThink = toolCalls.stream().filter(toolCall -> toolCall.name().equals("thinkTool")).count() > 0;
            if (!researchComplete.isEmpty() || toolCalls.isEmpty()) {
                log.info("Research completed by Supervisor Agent: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                supervisorAgentContext.setStatus(SupervisorAgentState.COMPLETED);
                break;
            }

            long conductResearch = toolCalls.stream().filter(tc -> tc.name().equals("conductResearch")).count();

            if (conductResearch > supervisorAgentContext.getMaxSubAgentsNumber()) {
                log.warn("Reached max sub-agents limit: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                chatMemory.add(supervisorId, new UserMessage("一次启动的数量过多，请减少一次启动的子智能体数量。"));
            } else if (conductResearch > 0 && isThink) {
                log.warn("Cannot conduct research and think at the same time: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                chatMemory.add(supervisorId, new UserMessage("不能同时进行研究和反思，请选择其一。"));
            } else {
                ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
                log.info("Supervisor Agent executed tool calls: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                Message message = executionResult.conversationHistory().getLast();
                chatMemory.add(supervisorId, message);
                if (!isThink) {
                    // 如果不是思考，则把子任务的结果加入到笔记中,方便最后进行总结
                    supervisorAgentContext.getNotes().add(message.getText());
                }
            }

            promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);
            response = chatModel.call(promptWithMemory);
        }

        log.info("任务完成，准备启动总结: supervisorId={}, sessionId={}",
                supervisorId, supervisorAgentContext.getSessionId());
        // 最终响应处理，生成总结报告
        supervisorAgentContext.setStatus(SupervisorAgentState.COMPLETED);
    }

}
