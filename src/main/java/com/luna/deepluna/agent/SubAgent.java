package com.luna.deepluna.agent;

import com.luna.deepluna.agent.agentTool.SubAgentTools;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.service.SessionProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubAgent {

    private final SubAgentTools subAgentTools;

    private final ChatClientCache chatClientCache;

    private final ToolCallingManager toolCallingManager;

    private final ContextCache contextCache;

    private final SessionProgressService sessionProgressService;

    public String startSubAgentResearch(String sessionId, String researchTopic) {
        AssertUtil.isNotEmpty(sessionId, "SessionId不能为空");
        SubAgentContext context = SubAgentContext.builder()
                .sessionId(sessionId)
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(400)
                        .build())
                .researchTopic(researchTopic)
                .maxWebSearch(5)
                .status(SubAgentTaskStatus.PENDING)
                .subAgentId(UUID.randomUUID().toString())
                .build();
        contextCache.putSubAgent(context.getSubAgentId(), context);
        sessionProgressService.publishSubAgentCreated(sessionId, context.getSubAgentId(), researchTopic);
        return subAgent(context.getSubAgentId());
    }

    private String subAgent(String subAgentId) {
        SubAgentContext subAgent = contextCache.getSubAgent(subAgentId);
        AssertUtil.isNotNull(subAgent, "Sub Agent not found: subAgentId=" + subAgentId);
        AssertUtil.isNotEmpty(subAgent.getSessionId(), "Sub Agent缺少关联sessionId: subAgentId=" + subAgentId);

        OpenAiChatModel chatModel = chatClientCache.getBySessionId(subAgent.getSessionId());
        AssertUtil.isNotNull(chatModel, "Chat model not found for Sub Agent: sessionId=" + subAgent.getSessionId());

        ChatMemory chatMemory = subAgent.getChatMemory();

        chatMemory.add(subAgentId, new AssistantMessage(Prompts.SUB_AGENT_PROMPT.formatted(LocalDateTime.now())));
        chatMemory.add(subAgentId, new UserMessage("Research Topic" + subAgent.getResearchTopic()));
        try {
            subAgent.setStatus(SubAgentTaskStatus.IN_PROGRESS);
            log.info("Sub Agent started: subAgentId={}", subAgentId);
            sessionProgressService.publishSubAgentStatus(
                    subAgent.getSessionId(),
                    subAgentId,
                    subAgent.getResearchTopic(),
                    SubAgentTaskStatus.IN_PROGRESS,
                    "子任务开始研究: " + subAgent.getResearchTopic()
            );

            Map<String, Object> webSearchUsage = new HashMap<>();
            webSearchUsage.put("count", new AtomicInteger(0));
            webSearchUsage.put("max", subAgent.getMaxWebSearch());

            ChatOptions chatOptions = ToolCallingChatOptions.builder()
                    .toolCallbacks(ToolCallbacks.from(subAgentTools))
                    .toolContext(webSearchUsage)
                    .internalToolExecutionEnabled(false)
                    .build();

            Prompt promptWithMemory = new Prompt(chatMemory.get(subAgentId), chatOptions);

            ChatResponse response = chatModel.call(promptWithMemory);
            while (response.hasToolCalls()) {
                Generation result = response.getResult();
                chatMemory.add(subAgentId, result.getOutput());
                List<AssistantMessage.ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();

                // 统计 webSearch 工具调用次数
                log.info("Sub Agent executing tool calls: subAgentId={}, toolCalls={}", subAgentId, toolCalls);

                ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
                log.info("Sub Agent received tool execution result: subAgentId={}, toolResults={}",
                        subAgentId, executionResult.conversationHistory().getLast());
                chatMemory.add(subAgentId, executionResult.conversationHistory().getLast());

                promptWithMemory = new Prompt(chatMemory.get(subAgentId), chatOptions);
                response = chatModel.call(promptWithMemory);
            }

            // 最终响应处理（压缩阶段不直接透传 tool role 消息，避免消息序列校验失败）
            log.info("Sub Agent starting compression: subAgentId={}", subAgentId);
            String transcript = buildCompressionTranscript(chatMemory.get(subAgentId));
            Prompt compressionPrompt = new Prompt(List.of(
                    new SystemMessage(Prompts.COMPRESS_RESEARCH_SYSTEM_PROMPT.formatted(LocalDateTime.now())),
                    new UserMessage("以下是本次研究过程记录，请严格按要求整理：\n\n" + transcript)
            ));
            Generation result = chatModel.call(compressionPrompt).getResult();
            String compressResp = result.getOutput().getText();
            AssertUtil.isFalse(compressResp == null || compressResp.isEmpty(), "压缩结果为空");
            subAgent.setStatus(SubAgentTaskStatus.COMPLETED);
            log.info("Sub Agent completed: subAgentId={}", subAgentId);
            sessionProgressService.publishSubAgentStatus(
                    subAgent.getSessionId(),
                    subAgentId,
                    subAgent.getResearchTopic(),
                    SubAgentTaskStatus.COMPLETED,
                    "子任务已完成: " + subAgent.getResearchTopic()
            );
            return compressResp;
        } catch (Exception ex) {
            subAgent.setStatus(SubAgentTaskStatus.FAILED);
            sessionProgressService.publishSubAgentStatus(
                    subAgent.getSessionId(),
                    subAgentId,
                    subAgent.getResearchTopic(),
                    SubAgentTaskStatus.FAILED,
                    "子任务执行失败: " + subAgent.getResearchTopic()
            );
            throw ex instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new RuntimeException(ex);
        }
    }

    private String buildCompressionTranscript(List<Message> messages) {
        StringBuilder sb = new StringBuilder(4096);
        for (Message message : messages) {
            String text = message.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            sb.append("[")
                    .append(message.getMessageType())
                    .append("] ")
                    .append(text)
                    .append("\n\n");
        }
        return sb.toString();
    }
}
