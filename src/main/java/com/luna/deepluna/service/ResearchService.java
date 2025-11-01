package com.luna.deepluna.service;

import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.agent.agentTool.SupervisorTools;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchService {

    private final SupervisorTools supervisorTools;

    private final DeepSeekChatModel chatModel;

    private final ToolCallingManager toolCallingManager;

    private final ContextCache contextCache;

    public void startResearch(SupervisorAgentContext supervisorAgentContext) {
        String researchBrief = supervisorAgentContext.getResearchBrief();
        String supervisorId = UUID.randomUUID().toString();
        supervisorAgentContext.setSupervisorId(supervisorId);
        supervisorAgentContext.setStatus(SupervisorAgentState.INITIALIZING);
        supervisorAgentContext.setChatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .build());
        contextCache.putSupervisor(supervisorAgentContext.getSessionId(), supervisorAgentContext);
        startSupervisorAgent(supervisorAgentContext.getSessionId());
    }

    private void startSupervisorAgent(String sessionId) {
        SupervisorAgentContext supervisorAgentContext = contextCache.getSupervisor(sessionId);
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
        while (response.hasToolCalls()) {
            Generation result = response.getResult();
            // 记录调用工具信息到对话记忆
            chatMemory.add(supervisorId, response.getResult().getOutput());
            List<AssistantMessage.ToolCall> toolCalls = result.getOutput().getToolCalls();
            List<AssistantMessage.ToolCall> researchComplete = toolCalls.stream().filter(toolCall -> toolCall.name().equals("researchComplete")).toList();
            if (!researchComplete.isEmpty()) {
                log.info("Research completed by Supervisor Agent: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                supervisorAgentContext.setStatus(SupervisorAgentState.COMPLETED);
                break;
            }
            long conductResearch = toolCalls.stream().filter(tc -> tc.name().equals("conductResearch")).count();
            if (conductResearch > supervisorAgentContext.getMaxSubAgentsNumber()) {
                log.warn("Reached max sub-agents limit: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                chatMemory.add(supervisorId, new AssistantMessage("一次启动的数量过多，请减少一次启动的子智能体数量。"));
            } else {
                ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
                chatMemory.add(supervisorId, executionResult.conversationHistory().getLast());
            }

            promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);
            response = chatModel.call(promptWithMemory);
        }

    }

}
