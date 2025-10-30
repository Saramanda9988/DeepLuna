package com.luna.deepluna.service;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.luna.deepluna.common.client.SseTransportClient;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.dto.entity.ChatHistory;
import com.luna.deepluna.dto.entity.Session;
import com.luna.deepluna.dto.jsonConvert.ClarifyResult;
import com.luna.deepluna.dto.request.ChatRequest;
import com.luna.deepluna.dto.response.ChatResp;
import com.luna.deepluna.dto.response.ClarifyChatResponse;
import com.luna.deepluna.dto.response.StreamChatResponse;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.repository.ChatHistoryRepository;
import com.luna.deepluna.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @JsonPropertyOrder({ "isClear" })
    private final BeanOutputConverter<ClarifyResult> checkConverter = new BeanOutputConverter<>(ClarifyResult.class);

    private final ChatMemory chatMemory;
    private final DeepSeekChatModel chatModel;
    private final SessionRepository sessionRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SseTransportClient sseTransportClient;
    private final ResearchService researchService;

    private final ConcurrentMap<String, Integer> sessionChatRounds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Session> activeSessions = new ConcurrentHashMap<>();

    /**
     * 处理流式聊天请求
     */
    @Transactional
    public void processChatStream(ChatRequest request, SseEmitter emitter) {
        log.info("Processing streaming chat request for sessionId: {}, message: {}", request.getSessionId(), request.getMessage());

        try {
            // 1. 检查session是否存在
            Session session = getSession(request);

            AssertUtil.isTrue(
                    Objects.equals(session.getStatus(), SessionStatus.IDLE) ||
                    Objects.equals(session.getStatus(), SessionStatus.CLARIFYING),
                    "当前Session状态不允许新的请求"
            );

            log.info("更新会话 {} 状态: {}", session.getSessionId(), session.getStatus());
            updateSessionStatus(session, SessionStatus.CLARIFYING);

            // 发送状态更新
            sseTransportClient.sendMessage(emitter, StreamChatResponse.builder()
                            .sessionId(session.getSessionId())
                            .streamFinished(false)
                            .sessionStatus(SessionStatus.CLARIFYING)
                            .build());

            // 3. 判断问题是否清晰
            boolean isClear = checkQuestionClarity(request.getMessage(), session.getSessionId());

            if (!isClear) {
                // 问题不清晰，请求用户补充
                handleUnclearQuestionStream(session, request.getMessage(), emitter);
            } else {
                // 问题清晰
                handleClearQuestionStream(session, request.getMessage(), emitter);
            }

        } catch (BusinessException be) {
            log.error("Business exception in streaming chat process", be);
            sseTransportClient.handleError(emitter, be);

        } catch (Exception e) {
            log.error("Error in streaming chat process", e);
            sseTransportClient.handleError(emitter, e);
        }
    }

    /**
     * 获取Session
     * @param request ChatRequest
     * @return session
     */
    private Session getSession(ChatRequest request) {
        if (Objects.isNull(request.getSessionId())) {
            throw new BusinessException("SessionId不能为空");
        }
        String sessionId = request.getSessionId();
        Session session = activeSessions.get(sessionId);
        if (session == null) {
            Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
            session = sessionOpt.orElseThrow(() -> new BusinessException("Session不存在"));
            activeSessions.put(sessionId, session);
        }
        return session;
    }

    /**
     * 获取澄清轮数
     */
    private Integer getChatRoundNumber(String sessionId) {
        Integer i = sessionChatRounds.get(sessionId);
        if (i == null) {
            ChatHistory latestHistory = chatHistoryRepository.findTopBySessionIdOrderByRoundNumberDesc(sessionId);
            i =  latestHistory != null ? latestHistory.getRoundNumber() : 0;
            sessionChatRounds.put(sessionId, i);
        }
        return i;
    }

    /**
     * 检查问题是否清晰
     */
    private boolean checkQuestionClarity(String message, String sessionId) {
        log.info("Checking question clarity for message: {}", message);

        List<Message> histories = chatMemory.get(sessionId);
        histories = new ArrayList<>(histories);
        histories.add(new UserMessage(Prompts.JUDGE_PROMPT.formatted(LocalDateTime.now(), message)));

        Generation response = chatModel.call(new Prompt(histories)).getResult();
        String text = response.getOutput().getText();
        AssertUtil.isNotNull(text, "AI未返回澄清结果");
        ClarifyResult convert = checkConverter.convert(text);

        log.info("Clarity check result: {}", convert.toString());

        return convert.isClear();
    }

    /**
     * 保存澄清历史
     */
    @Async
    protected void saveChatHistory(String sessionId, String question, String answer, boolean completed) {
        ChatHistory history = ChatHistory.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .question(question)
                .answer(answer)
                .roundNumber(getChatRoundNumber(sessionId) + 1)
                .completed(completed)
                .build();

        chatHistoryRepository.save(history);
        log.info("Saved Chat history for sessionId: {}, round: {}, completed: {}",
                sessionId, history.getRoundNumber(), completed);
        chatMemory.add(sessionId, new UserMessage(question));
        chatMemory.add(sessionId, new AssistantMessage(answer));
    }

    /**
     * 流式处理不清晰的问题
     */
    private void handleUnclearQuestionStream(Session session, String message, SseEmitter emitter) {
        log.info("Handling unclear question for sessionId: {}", session.getSessionId());

        List<Message> histories = chatMemory.get(session.getSessionId());
        histories = new ArrayList<>(histories);
        histories.add(new UserMessage(Prompts.CLARIFY_PROMPT.formatted(message)));

        // 使用流式调用
        Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(histories));
        handleStreamingResponse(responseFlux, session, message, emitter);
    }

    /**
     * 流式处理清晰的问题
     */
    private void handleClearQuestionStream(Session session, String message, SseEmitter emitter) {
        log.info("Handling clear question for sessionId: {}", session.getSessionId());

        List<Message> histories = chatMemory.get(session.getSessionId());
        histories = new ArrayList<>(histories);

        // 生成研究简报
        histories = chatMemory.get(session.getSessionId());
        histories = new ArrayList<>(histories);
        histories.add(new UserMessage(Prompts.BRIEF_PROMPT.formatted(LocalDateTime.now())));

        Generation response = chatModel.call(new Prompt(histories)).getResult();
        String researchBrief = response.getOutput().getText();
        AssertUtil.isNotNull(researchBrief, "AI未返回澄清结果");
        histories.add(new AssistantMessage(researchBrief));

        // 生成总结回复
        histories.add(new UserMessage(Prompts.SUMMARY_PROMPT.formatted(message)));
        Flux<ChatResponse> stream = chatModel.stream(new Prompt(histories));

        handleStreamingResponse(stream, session, message, emitter);

        // 保存研究简报到session
        session.setResearchBrief(researchBrief);
        sessionRepository.save(session);
        log.info("Saved research brief for sessionId: {}", session.getSessionId());

        // TODO: 在这里启动sub-agent执行具体的研究任务

        SupervisorAgentContext supervisorAgentContext = SupervisorAgentContext.builder()
                .sessionId(session.getSessionId())
                .researchBrief(researchBrief)
                .maxSubAgentsNumber(10L)
                .build();

        researchService.startResearch(supervisorAgentContext);

        updateSessionStatus(session, SessionStatus.RUNNING);
    }

    /**
     * 处理流式响应
     */
    private void handleStreamingResponse(Flux<ChatResponse> responseFlux,
                                       Session session, String message, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        responseFlux.subscribe(
            response -> {
                String content = response.getResult().getOutput().getText();
                fullResponse.append(content);
                // 发送流式内容
                sseTransportClient.sendMessage(emitter, ChatResp.builder()
                                .sessionId(session.getSessionId())
                                .sessionStatus(session.getStatus())
                                .message(content)
                                .build());
            },
            error -> {
                log.error("Error in streaming clarification", error);
                throw new BusinessException(error.toString());
            },
            () -> {
                // 发送完整的聊天响应
                ClarifyChatResponse response = ClarifyChatResponse.builder()
                        .sessionId(session.getSessionId())
                        .message(fullResponse.toString())
                        .sessionStatus(SessionStatus.CLARIFYING)
                        .needsClarification(false)
                        .build();

                sseTransportClient.sendEndMessage(emitter, response);

                // 保存澄清流程
                saveChatHistory(session.getSessionId(), message, fullResponse.toString(), true);
            }
        );
    }

    private void updateSessionStatus(Session session, SessionStatus status) {
        log.info("Updating session {} status to {}", session.getSessionId(), status);
        AssertUtil.isNotNull(session, "Session不能为空");
        session.setStatus(status);

        // TODO: 使用@Async注解控制的受控线程池异步保存
        CompletableFuture.runAsync(() -> {
            sessionRepository.save(session);
            log.info("Session {} status updated to {}", session.getSessionId(), status);
        });
    }
}