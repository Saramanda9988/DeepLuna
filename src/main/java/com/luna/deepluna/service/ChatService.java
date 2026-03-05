package com.luna.deepluna.service;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.SessionCache;
import com.luna.deepluna.common.client.SseTransportClient;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.ChatHistory;
import com.luna.deepluna.domain.entity.Model;
import com.luna.deepluna.domain.entity.Session;
import com.luna.deepluna.domain.request.ChatRequest;
import com.luna.deepluna.domain.response.ChatResp;
import com.luna.deepluna.domain.response.ClarifyChatResponse;
import com.luna.deepluna.event.StartResearchEvent;
import com.luna.deepluna.repository.ChatHistoryRepository;
import com.luna.deepluna.repository.SessionRepository;
import com.luna.deepluna.service.factory.CustomModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    public record ClarifyResult(Boolean isClear) {}

    @JsonPropertyOrder({ "isClear" })
    private final BeanOutputConverter<ClarifyResult> checkConverter = new BeanOutputConverter<>(ClarifyResult.class);

    private final ChatMemory chatMemory;

    private final ModelService modelService;
    private final CustomModelFactory customModelFactory;

    private final SessionRepository sessionRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SseTransportClient sseTransportClient;

    private final SessionCache sessionCache;
    private final ChatClientCache chatClientCache;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Qualifier("chatHistoryExecutor")
    private final Executor chatHistoryExecutor;

    @Qualifier("persistenceExecutor")
    private final Executor persistenceExecutor;

    /**
     * 处理流式聊天请求
     */
    public void processChat(ChatRequest request, SseEmitter emitter) {
        log.info("Processing streaming chat request for sessionId: {}, message: {}", request.getSessionId(), request.getMessage());

        // 检查session是否存在
        Session session = getSession(request);

        AssertUtil.isTrue(
                Objects.equals(session.getStatus(), SessionStatus.IDLE) ||
                        Objects.equals(session.getStatus(), SessionStatus.CLARIFYING) ||
                        Objects.equals(session.getStatus(), SessionStatus.FAILED),
                "当前Session状态不允许新的请求"
        );
        Model model = modelService.getModelEntityById(request.getModelId());
        OpenAiChatModel chatModel = customModelFactory.createChatModelClient(model);
        chatClientCache.putBySessionId(session.getSessionId(), chatModel);

        try {
            // 判断问题是否清晰
            updateSessionStatus(session, SessionStatus.CLARIFYING);
            // 检查问题清晰度
            boolean isClear = checkQuestionClarity(request.getMessage(), session.getSessionId());
            if (!isClear) {
                // 问题不清晰，请求用户补充
                handleUnclearQuestionStream(session, request.getMessage(), emitter);
            } else {
                // 问题清晰，事件将在流完成后发布
                handleClearQuestionStream(session, request.getMessage(), emitter);
            }

        } catch (BusinessException be) {
            log.error("Business exception in chat process", be);
            updateSessionStatus(session, SessionStatus.FAILED);
            sseTransportClient.handleError(emitter, be);
            throw be;
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
        Session session = sessionCache.getActiveSession(sessionId);
        if (session == null) {
            Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
            session = sessionOpt.orElseThrow(() -> new BusinessException("Session不存在"));
            sessionCache.putActiveSession(sessionId, session);
        }
        return session;
    }

    /**
     * 获取澄清轮数
     */
    private Integer getChatRoundNumber(String sessionId) {
        Integer i = sessionCache.getChatRounds(sessionId);
        if (i == null) {
            ChatHistory latestHistory = chatHistoryRepository.findTopBySessionIdOrderByRoundNumberDesc(sessionId);
            i =  latestHistory != null ? latestHistory.getRoundNumber() : 0;
            sessionCache.putChatRounds(sessionId, i);
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

        OpenAiChatModel chatModel = chatClientCache.getBySessionId(sessionId);
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
    private void saveChatHistorySync(String sessionId, String question, String answer, boolean completed) {
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
        OpenAiChatModel chatModel = chatClientCache.getBySessionId(session.getSessionId());
        Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(histories));
        asyncHandleStreamingResponse(responseFlux, session, message, emitter, false);
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

        OpenAiChatModel chatModel = chatClientCache.getBySessionId(session.getSessionId());
        Generation response = chatModel.call(new Prompt(histories)).getResult();
        String researchBrief = response.getOutput().getText();
        AssertUtil.isNotNull(researchBrief, "AI未返回澄清结果");
        histories.add(new AssistantMessage(researchBrief));

        // 生成总结回复
        // 保存研究简报到session，先提交数据以避免后台异步线程依赖未提交的事务/实体状态
        session.setResearchBrief(researchBrief);
        sessionRepository.saveAndFlush(session);
        log.info("Saved research brief for sessionId: {}", session.getSessionId());

        histories.add(new UserMessage(Prompts.SUMMARY_PROMPT.formatted(message)));

        Flux<ChatResponse> stream = chatModel.stream(new Prompt(histories));
        asyncHandleStreamingResponse(stream, session, message, emitter, true);
    }
    
    /**
     * 处理流式响应
     * @param shouldPublishEvent 是否在完成后发布研究事件
     */
    private void asyncHandleStreamingResponse(Flux<ChatResponse> responseFlux,
                                       Session session, String message, SseEmitter emitter, boolean shouldPublishEvent) {
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
                CompletableFuture.runAsync(
                                () -> saveChatHistorySync(session.getSessionId(), message, fullResponse.toString(), true),
                                chatHistoryExecutor)
                        .exceptionally(ex -> {
                            log.error("Failed to persist chat history for sessionId={}", session.getSessionId(), ex);
                            return null;
                        });
                
                // 如果需要，在流完成后发布研究事件
                if (shouldPublishEvent) {
                    log.info("Publishing StartResearchEvent for sessionId: {}", session.getSessionId());
                    applicationEventPublisher.publishEvent(new StartResearchEvent(this, session.getSessionId()));
                }
            }
        );
    }

    private void updateSessionStatus(Session session, SessionStatus status) {
        log.info("Updating session {} status to {}", session.getSessionId(), status);
        AssertUtil.isNotNull(session, "Session不能为空");
        session.setStatus(status);

        CompletableFuture.runAsync(() -> {
                    sessionRepository.save(session);
                    log.info("Session {} status updated to {}", session.getSessionId(), status);
                }, persistenceExecutor)
                .exceptionally(ex -> {
                    log.error("Failed to persist session status: sessionId={}, status={}",
                            session.getSessionId(), status, ex);
                    return null;
                });
    }
}
