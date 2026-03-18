package com.luna.deepluna.agent;

import com.luna.deepluna.agent.agentTool.SupervisorTools;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisorAgent {

    private final SupervisorTools supervisorTools;
    private final SubAgent subAgent;
    private final ChatClientCache chatClientCache;
    private final ToolCallingManager toolCallingManager;
    private final ContextCache contextCache;
    private final SessionProgressService sessionProgressService;

    public void startResearch(String sessionId, String researchBrief) {
        SupervisorAgentContext supervisorAgentContext = SupervisorAgentContext.builder()
                .supervisorId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .researchBrief(researchBrief)
                .maxSubAgentsNumber(5L)
                .status(SupervisorAgentState.INITIALIZING)
                .notes(new ArrayList<>())
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(600)
                        .build())
                .build();
        contextCache.putSupervisor(supervisorAgentContext.getSessionId(), supervisorAgentContext);
        sessionProgressService.publishSupervisorStatus(sessionId, SupervisorAgentState.INITIALIZING, "Supervisor 已初始化");
        try {
            supervisorAgent(supervisorAgentContext.getSessionId());
        } catch (Exception ex) {
            supervisorAgentContext.setStatus(SupervisorAgentState.FAILED);
            sessionProgressService.publishSupervisorStatus(sessionId, SupervisorAgentState.FAILED, "Supervisor 执行失败");
            throw ex instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new RuntimeException(ex);
        }
    }

    private void supervisorAgent(String sessionId) {
        OpenAiChatModel chatModel = chatClientCache.getBySessionId(sessionId);
        AssertUtil.isNotNull(chatModel, "Chat model not found for sessionId: " + sessionId);

        SupervisorAgentContext supervisorAgentContext = contextCache.getSupervisor(sessionId);
        AssertUtil.isNotNull(supervisorAgentContext, "SupervisorAgentContext not found for sessionId: " + sessionId);

        String supervisorId = supervisorAgentContext.getSupervisorId();
        ChatMemory chatMemory = supervisorAgentContext.getChatMemory();

        chatMemory.add(supervisorId, new AssistantMessage(Prompts.SUPERVISOR_PROMPT.formatted(
                LocalDateTime.now(),
                5,
                supervisorAgentContext.getMaxSubAgentsNumber()
        )));
        chatMemory.add(supervisorId, new UserMessage("Research Brief:" + supervisorAgentContext.getResearchBrief()));

        log.info("Supervisor Agent started: supervisorId={}, sessionId={}", supervisorId, sessionId);
        supervisorAgentContext.setStatus(SupervisorAgentState.RUNNING);
        sessionProgressService.publishSupervisorStatus(sessionId, SupervisorAgentState.RUNNING, "Supervisor 正在拆解研究任务");

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(supervisorTools))
                .toolContext(Map.of("sessionId", sessionId))
                .internalToolExecutionEnabled(false)
                .build();

        Prompt promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);
        ChatResponse response = chatModel.call(promptWithMemory);
        int turn = 0;
        final int maxTurns = 80;

        while (response.hasToolCalls()) {
            turn++;
            if (turn > maxTurns) {
                throw new IllegalStateException("Supervisor循环超过最大轮次限制: " + maxTurns);
            }

            Generation result = response.getResult();
            List<AssistantMessage.ToolCall> toolCalls = result.getOutput().getToolCalls();

            log.info("Supervisor Agent received tool calls: supervisorId={}, sessionId={}, toolCalls={}",
                    supervisorId, sessionId, toolCalls);
            chatMemory.add(supervisorId, result.getOutput());

            boolean hasResearchComplete = toolCalls.stream().anyMatch(tc -> "researchComplete".equals(tc.name()));
            boolean hasConductResearch = toolCalls.stream().anyMatch(tc -> "conductResearch".equals(tc.name()));
            if (hasResearchComplete) {
                // 先执行 researchComplete，补齐 tool_call -> tool_response 的消息序列
                ToolExecutionResult completeResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
                chatMemory.add(supervisorId, completeResult.conversationHistory().getLast());

                int running = countRunningSubAgents(sessionId);
                if (running > 0) {
                    chatMemory.add(supervisorId, new UserMessage(
                            "当前仍有子任务运行中，暂不能结束研究。\n"
                                    + buildSubAgentStateSummary(sessionId)
                                    + "\n如果你已经确认信息充足，可先调用 stopAllRunningSubAgents 停止剩余任务，再调用 researchComplete。"
                                    + "\n接下来会阻塞等待一个子任务回包。"
                    ));
                    awaitAnySubAgentResult(supervisorAgentContext, 120000L);
                    collectSubAgentUpdates(supervisorAgentContext, true);
                    promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);
                    response = chatModel.call(promptWithMemory);
                    continue;
                }

                collectSubAgentUpdates(supervisorAgentContext, true);
                supervisorAgentContext.setStatus(SupervisorAgentState.COMPLETED);
                sessionProgressService.publishSupervisorStatus(sessionId, SupervisorAgentState.COMPLETED, "Supervisor 已完成研究调度");
                break;
            }

            ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
            Message message = executionResult.conversationHistory().getLast();
            chatMemory.add(supervisorId, message);
            log.info("Supervisor Agent executed tool calls: supervisorId={}, sessionId={}", supervisorId, sessionId);

            collectSubAgentUpdates(supervisorAgentContext, false);
            if (hasConductResearch) {
                awaitAnySubAgentResult(supervisorAgentContext, 120000L);
                collectSubAgentUpdates(supervisorAgentContext, true);
            }

            promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);
            response = chatModel.call(promptWithMemory);
        }

        log.info("任务完成，准备启动总结: supervisorId={}, sessionId={}", supervisorId, sessionId);
        if (supervisorAgentContext.getStatus() != SupervisorAgentState.COMPLETED) {
            collectSubAgentUpdates(supervisorAgentContext, true);
            supervisorAgentContext.setStatus(SupervisorAgentState.COMPLETED);
            sessionProgressService.publishSupervisorStatus(sessionId, SupervisorAgentState.COMPLETED, "Supervisor 已完成研究调度");
        }
    }

    private void collectSubAgentUpdates(SupervisorAgentContext context, boolean forceSnapshot) {
        List<SubAgentContext> subAgents = contextCache.getSubAgentsBySessionId(context.getSessionId());
        if (subAgents.isEmpty()) {
            return;
        }

        List<String> newCompleted = new ArrayList<>();
        List<String> newFailed = new ArrayList<>();
        int pending = 0;
        int running = 0;
        int completed = 0;
        int failed = 0;

        for (SubAgentContext subAgent : subAgents) {
            SubAgentTaskStatus status = subAgent.getStatus();
            if (status == null) {
                continue;
            }
            if (status == SubAgentTaskStatus.PENDING) {
                pending++;
            } else if (status == SubAgentTaskStatus.IN_PROGRESS) {
                running++;
            } else if (status == SubAgentTaskStatus.COMPLETED) {
                completed++;
                if (context.getCollectedSubAgentIds().add(subAgent.getSubAgentId())) {
                    if (subAgent.getResult() != null && !subAgent.getResult().isBlank()) {
                        context.getNotes().add(subAgent.getResult());
                    }
                    newCompleted.add(formatSubAgentSummary(subAgent));
                }
            } else if (status == SubAgentTaskStatus.FAILED) {
                failed++;
                if (context.getCollectedSubAgentIds().add(subAgent.getSubAgentId())) {
                    newFailed.add(formatSubAgentSummary(subAgent));
                }
            }
        }

        if (!forceSnapshot && newCompleted.isEmpty() && newFailed.isEmpty()) {
            return;
        }

        StringBuilder update = new StringBuilder(512);
        update.append("子任务进度更新：")
                .append("PENDING=").append(pending)
                .append(", RUNNING=").append(running)
                .append(", COMPLETED=").append(completed)
                .append(", FAILED=").append(failed)
                .append("\n");

        if (!newCompleted.isEmpty()) {
            update.append("本轮完成：\n");
            newCompleted.forEach(item -> update.append("- ").append(item).append("\n"));
        }
        if (!newFailed.isEmpty()) {
            update.append("本轮失败：\n");
            newFailed.forEach(item -> update.append("- ").append(item).append("\n"));
            update.append("请判断是否调用 restartFailedSubAgent 重启失败任务，或在信息足够时直接停止剩余任务。\n");
        }

        context.getChatMemory().add(context.getSupervisorId(), new UserMessage(update.toString()));
    }

    private String buildSubAgentStateSummary(String sessionId) {
        List<SubAgentContext> subAgents = contextCache.getSubAgentsBySessionId(sessionId);
        if (subAgents.isEmpty()) {
            return "当前无子任务。";
        }

        int pending = 0;
        int running = 0;
        int completed = 0;
        int failed = 0;
        for (SubAgentContext subAgent : subAgents) {
            SubAgentTaskStatus status = subAgent.getStatus();
            if (status == SubAgentTaskStatus.PENDING) {
                pending++;
            } else if (status == SubAgentTaskStatus.IN_PROGRESS) {
                running++;
            } else if (status == SubAgentTaskStatus.COMPLETED) {
                completed++;
            } else if (status == SubAgentTaskStatus.FAILED) {
                failed++;
            }
        }
        return "子任务状态汇总: PENDING=" + pending + ", RUNNING=" + running
                + ", COMPLETED=" + completed + ", FAILED=" + failed;
    }

    private int countRunningSubAgents(String sessionId) {
        List<SubAgentContext> subAgents = contextCache.getSubAgentsBySessionId(sessionId);
        return (int) subAgents.stream()
                .map(SubAgentContext::getStatus)
                .filter(status -> status == SubAgentTaskStatus.PENDING || status == SubAgentTaskStatus.IN_PROGRESS)
                .count();
    }

    private String formatSubAgentSummary(SubAgentContext subAgent) {
        String topic = subAgent.getResearchTopic() == null ? "" : subAgent.getResearchTopic();
        if (topic.length() > 80) {
            topic = topic.substring(0, 80) + "...";
        }
        String error = subAgent.getErrorMessage() == null ? "" : (" | error=" + subAgent.getErrorMessage());
        return "subAgentId=" + subAgent.getSubAgentId()
                + " | status=" + subAgent.getStatus()
                + " | topic=" + topic
                + error;
    }

    private void awaitAnySubAgentResult(SupervisorAgentContext context, long timeoutMs) {
        Map<String, Object> event = subAgent.waitForAnySubAgentResult(context.getSessionId(), timeoutMs);
        String eventType = toStringValue(event.get("eventType"));
        if ("SUB_AGENT_RESULT".equals(eventType)) {
            String status = toStringValue(event.get("status"));
            String subAgentId = toStringValue(event.get("subAgentId"));
            String topic = toStringValue(event.get("researchTopic"));
            String preview = toStringValue(event.get("resultPreview"));
            String error = toStringValue(event.get("errorMessage"));
            StringBuilder feedback = new StringBuilder(512);
            feedback.append("收到子任务回包：")
                    .append("subAgentId=").append(subAgentId)
                    .append(", status=").append(status)
                    .append(", topic=").append(topic)
                    .append("\n");
            if (!preview.isBlank()) {
                feedback.append("结果摘要：\n").append(preview).append("\n");
            }
            if (!error.isBlank()) {
                feedback.append("错误信息：").append(error).append("\n");
            }
            feedback.append("请先用 thinkTool 评估是否信息已足够；若足够可停止剩余任务并进入完成。");
            context.getChatMemory().add(context.getSupervisorId(), new UserMessage(feedback.toString()));
            return;
        }

        context.getChatMemory().add(context.getSupervisorId(), new UserMessage(
                "等待子任务回包事件：" + toStringValue(event.get("message"))
        ));
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
