package com.luna.deepluna.agent;

import com.luna.deepluna.agent.agentTool.SubAgentTools;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.service.SessionProgressService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class SubAgent {

    private final SubAgentTools subAgentTools;

    private final ChatClientCache chatClientCache;

    private final ToolCallingManager toolCallingManager;

    private final ContextCache contextCache;

    private final SessionProgressService sessionProgressService;

    private final Executor agentExecutor;

    private final ConcurrentMap<String, LinkedBlockingQueue<SubAgentResultEvent>> sessionResultQueues = new ConcurrentHashMap<>();

    public SubAgent(
            SubAgentTools subAgentTools,
            ChatClientCache chatClientCache,
            ToolCallingManager toolCallingManager,
            ContextCache contextCache,
            SessionProgressService sessionProgressService,
            @Qualifier("agentExecutor") Executor agentExecutor
    ) {
        this.subAgentTools = subAgentTools;
        this.chatClientCache = chatClientCache;
        this.toolCallingManager = toolCallingManager;
        this.contextCache = contextCache;
        this.sessionProgressService = sessionProgressService;
        this.agentExecutor = agentExecutor;
    }

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

        CompletableFuture.runAsync(() -> subAgent(context.getSubAgentId()), agentExecutor)
                .exceptionally(ex -> {
                    log.error("Sub Agent async execution failed unexpectedly: subAgentId={}", context.getSubAgentId(), ex);
                    return null;
                });

        return context.getSubAgentId();
    }

    public Map<String, Object> getSubAgentStatus(String subAgentId) {
        AssertUtil.isNotEmpty(subAgentId, "subAgentId不能为空");
        SubAgentContext context = contextCache.getSubAgent(subAgentId);
        AssertUtil.isNotNull(context, "Sub Agent不存在: subAgentId=" + subAgentId);
        return buildStatusPayload(context, true);
    }

    public List<Map<String, Object>> listSessionSubAgentStatuses(String sessionId) {
        AssertUtil.isNotEmpty(sessionId, "sessionId不能为空");
        return contextCache.getSubAgentsBySessionId(sessionId)
                .stream()
                .map(context -> buildStatusPayload(context, false))
                .toList();
    }

    public Map<String, Object> waitForAnySubAgentResult(String sessionId, long timeoutMs) {
        AssertUtil.isNotEmpty(sessionId, "sessionId不能为空");
        long normalizedTimeout = timeoutMs <= 0 ? 60000L : timeoutMs;
        LinkedBlockingQueue<SubAgentResultEvent> queue =
                sessionResultQueues.computeIfAbsent(sessionId, key -> new LinkedBlockingQueue<>());
        try {
            SubAgentResultEvent event = queue.poll(normalizedTimeout, TimeUnit.MILLISECONDS);
            if (event == null) {
                return Map.of(
                        "eventType", "TIMEOUT",
                        "message", "在等待子任务结果时超时"
                );
            }
            return Map.of(
                    "eventType", "SUB_AGENT_RESULT",
                    "subAgentId", event.subAgentId(),
                    "researchTopic", event.researchTopic(),
                    "status", event.status(),
                    "errorMessage", event.errorMessage() == null ? "" : event.errorMessage(),
                    "hasResult", event.result() != null && !event.result().isBlank(),
                    "resultPreview", buildPreview(event.result(), 800)
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Map.of(
                    "eventType", "INTERRUPTED",
                    "message", "等待子任务结果时线程被中断"
            );
        }
    }

    public String getSubAgentResult(String subAgentId) {
        AssertUtil.isNotEmpty(subAgentId, "subAgentId不能为空");
        SubAgentContext context = contextCache.getSubAgent(subAgentId);
        AssertUtil.isNotNull(context, "Sub Agent不存在: subAgentId=" + subAgentId);
        AssertUtil.isTrue(context.getStatus() == SubAgentTaskStatus.COMPLETED, "该子任务尚未完成");
        return context.getResult();
    }

    public void cancelSubAgent(String subAgentId) {
        AssertUtil.isNotEmpty(subAgentId, "subAgentId不能为空");
        SubAgentContext context = contextCache.getSubAgent(subAgentId);
        AssertUtil.isNotNull(context, "Sub Agent不存在: subAgentId=" + subAgentId);
        context.setCancelled(true);
    }

    public int cancelRunningSubAgents(String sessionId) {
        AssertUtil.isNotEmpty(sessionId, "sessionId不能为空");
        int cancelCount = 0;
        for (SubAgentContext context : contextCache.getSubAgentsBySessionId(sessionId)) {
            if (context.getStatus() == SubAgentTaskStatus.PENDING || context.getStatus() == SubAgentTaskStatus.IN_PROGRESS) {
                context.setCancelled(true);
                cancelCount++;
            }
        }
        return cancelCount;
    }

    public String restartFailedSubAgent(String sessionId, String failedSubAgentId) {
        AssertUtil.isNotEmpty(sessionId, "sessionId不能为空");
        AssertUtil.isNotEmpty(failedSubAgentId, "failedSubAgentId不能为空");
        SubAgentContext failed = contextCache.getSubAgent(failedSubAgentId);
        AssertUtil.isNotNull(failed, "Sub Agent不存在: subAgentId=" + failedSubAgentId);
        AssertUtil.equal(sessionId, failed.getSessionId(), "Sub Agent与当前会话不匹配");
        AssertUtil.equal(SubAgentTaskStatus.FAILED, failed.getStatus(), "仅允许重启失败的子任务");
        return startSubAgentResearch(sessionId, failed.getResearchTopic());
    }

    private void subAgent(String subAgentId) {
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
            subAgent.setStartedTime(LocalDateTime.now());
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

            checkCancelled(subAgent);
            ChatResponse response = chatModel.call(promptWithMemory);
            while (response.hasToolCalls()) {
                checkCancelled(subAgent);
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
                checkCancelled(subAgent);
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
            subAgent.setResult(compressResp);
            subAgent.setErrorMessage(null);
            subAgent.setFinishedTime(LocalDateTime.now());
            publishResultEvent(subAgent, SubAgentTaskStatus.COMPLETED, compressResp, null);
            log.info("Sub Agent completed: subAgentId={}", subAgentId);
            sessionProgressService.publishSubAgentStatus(
                    subAgent.getSessionId(),
                    subAgentId,
                    subAgent.getResearchTopic(),
                    SubAgentTaskStatus.COMPLETED,
                    "子任务已完成: " + subAgent.getResearchTopic()
            );
        } catch (Exception ex) {
            subAgent.setStatus(SubAgentTaskStatus.FAILED);
            subAgent.setResult(null);
            subAgent.setErrorMessage(ex.getMessage());
            subAgent.setFinishedTime(LocalDateTime.now());
            publishResultEvent(subAgent, SubAgentTaskStatus.FAILED, null, ex.getMessage());
            sessionProgressService.publishSubAgentStatus(
                    subAgent.getSessionId(),
                    subAgentId,
                    subAgent.getResearchTopic(),
                    SubAgentTaskStatus.FAILED,
                    "子任务执行失败: " + subAgent.getResearchTopic()
            );
            throw ex instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(ex);
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

    private void checkCancelled(SubAgentContext subAgent) {
        if (subAgent.isCancelled()) {
            throw new RuntimeException("[CANCELLED] 子任务被Supervisor中止");
        }
    }

    private void publishResultEvent(
            SubAgentContext subAgent,
            SubAgentTaskStatus status,
            String result,
            String errorMessage
    ) {
        LinkedBlockingQueue<SubAgentResultEvent> queue =
                sessionResultQueues.computeIfAbsent(subAgent.getSessionId(), key -> new LinkedBlockingQueue<>());
        queue.offer(new SubAgentResultEvent(
                subAgent.getSubAgentId(),
                subAgent.getResearchTopic(),
                status,
                result,
                errorMessage
        ));
    }

    private Map<String, Object> buildStatusPayload(SubAgentContext context, boolean includeResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subAgentId", context.getSubAgentId());
        payload.put("sessionId", context.getSessionId());
        payload.put("researchTopic", context.getResearchTopic());
        payload.put("status", context.getStatus());
        payload.put("startedTime", context.getStartedTime());
        payload.put("finishedTime", context.getFinishedTime());
        payload.put("errorMessage", context.getErrorMessage());
        payload.put("hasResult", context.getResult() != null && !context.getResult().isBlank());
        if (includeResult && context.getResult() != null) {
            payload.put("result", context.getResult());
        } else {
            payload.put("resultPreview", buildPreview(context.getResult(), 600));
        }
        return payload;
    }

    private String buildPreview(String content, int maxChars) {
        if (content == null || content.isBlank()) {
            return "";
        }
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "...";
    }

    private record SubAgentResultEvent(
            String subAgentId,
            String researchTopic,
            SubAgentTaskStatus status,
            String result,
            String errorMessage
    ) {
    }
}
