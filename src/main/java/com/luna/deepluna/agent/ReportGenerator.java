package com.luna.deepluna.agent;

import com.luna.deepluna.agent.context.SupervisorAgentContext;
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
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerator {

    private final DeepSeekChatModel chatModel;
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
        List<Message> histories = new ArrayList<>(conversationHistories);
        histories.add(new UserMessage(Prompts.FINAL_REPORT_GENERATE_PROMPT.formatted(
                session.getResearchBrief(),
                histories.stream()
                        .filter(msg -> msg instanceof AssistantMessage)
                        .map(Message::getText)
                        .reduce("", (a, b) -> a + "\n- " + b),
                LocalDateTime.now(),
                String.join("\n", supervisor.getNotes())
        )));

        ChatResponse response = chatModel.call(new Prompt(histories));
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
