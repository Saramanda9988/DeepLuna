package com.luna.deepluna.agent;

import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.SessionCache;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.ChatHistory;
import com.luna.deepluna.domain.entity.Session;
import com.luna.deepluna.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerator {

    private final ChatClientCache chatClientCache;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SessionCache sessionCache;

    public void generateFinalReport(String sessionId, SupervisorAgentContext supervisor) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.error("Session ID is null or empty. Cannot generate final report.");
            throw new BusinessException("Session ID不能为空，无法生成最终报告");
        }

        Session session = sessionCache.getActiveSession(sessionId);
        AssertUtil.isTrue(Objects.nonNull(session) &&
                        Objects.equals(session.getStatus(), SessionStatus.REPORTING),
                    "Session状态不正确，无法生成最终报告");

        log.info("Generating final report for sessionId: {}", session.getSessionId());

        List<Message> conversationHistories = supervisor.getChatMemory().get(supervisor.getSupervisorId());
        String assistantTranscript = conversationHistories.stream()
                .filter(msg -> msg instanceof AssistantMessage)
                .map(Message::getText)
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (a, b) -> a + "\n- " + b);

        String notes = String.join("\n", supervisor.getNotes());
        String reportPrompt = Prompts.FINAL_REPORT_GENERATE_PROMPT.formatted(
                session.getResearchBrief(),
                assistantTranscript,
                LocalDateTime.now(),
                notes
        );

        OpenAiChatModel chatModel = chatClientCache.getBySessionId(sessionId);
        AssertUtil.isNotNull(chatModel, "Chat模型未初始化，无法生成最终报告");

        // 不直接传入 supervisor 原始 memory，避免包含 tool_calls 而触发上下文顺序校验错误
        ChatResponse response = chatModel.call(new Prompt(List.of(new UserMessage(reportPrompt))));
        Generation result = response.getResult();
        String report = result.getOutput().getText();
        AssertUtil.isNotNull(report, "AI未返回最终报告");

        ChatHistory history = ChatHistory.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .question("最终报告")
                .answer(report)
                .roundNumber(-1)
                .completed(true)
                .build();

        chatHistoryRepository.save(history);
        log.info("Final report generated and session completed for sessionId: {}", session.getSessionId());
    }
}
