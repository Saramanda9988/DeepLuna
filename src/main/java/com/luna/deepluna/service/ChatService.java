package com.luna.deepluna.service;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.luna.deepluna.common.client.SseTransportClient;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.dto.entity.ChatHistory;
import com.luna.deepluna.dto.entity.Session;
import com.luna.deepluna.dto.request.ChatRequest;
import com.luna.deepluna.dto.response.ChatResp;
import com.luna.deepluna.dto.response.ClarifyChatResponse;
import com.luna.deepluna.dto.response.StreamChatResponse;
import com.luna.deepluna.repository.ChatHistoryRepository;
import com.luna.deepluna.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    record ClarifyResult(Boolean isClear) {}

    record ClarifyQuestions(String question) {}

    @JsonPropertyOrder({ "isClear" })
    private final BeanOutputConverter<ClarifyResult> checkConverter = new BeanOutputConverter<>(ClarifyResult.class);

    private final BeanOutputConverter<ClarifyQuestions> questionConverter = new BeanOutputConverter<>(ClarifyQuestions.class);

    private final ChatMemory chatMemory;
    private final DeepSeekChatModel chatModel;
    private final SessionRepository sessionRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SseTransportClient sseTransportClient;

    private final ConcurrentMap<Session, Integer> sessionChatRounds = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Session> activeSessions = new ConcurrentHashMap<>();

    /**
     * 处理流式聊天请求
     */
    @Transactional
    public void processChatStream(ChatRequest request, SseEmitter emitter) {
        log.info("Processing streaming chat request for sessionId: {}, message: {}", request.getSessionId(), request.getMessage());

        try {
            // 1. 检查session是否存在
            Optional<Session> sessionOpt = sessionRepository.findById(request.getSessionId());
            Session session = sessionOpt.orElse(null);
            AssertUtil.isNotNull(session, "Session不存在");
            AssertUtil.isTrue(Objects.equals(session.getStatus(), SessionStatus.IDLE), "当前Session状态不允许新的请求");

            log.info("更新会话 {} 状态: {}", session.getSessionId(), session.getStatus());
            session.setStatus(SessionStatus.CLARIFYING);
            sessionRepository.save(session);

            // 发送状态更新
            sseTransportClient.sendMessage(emitter, StreamChatResponse.builder()
                            .sessionId(session.getSessionId())
                            .streamFinished(false)
                            .sessionStatus(SessionStatus.CLARIFYING)
                            .message("正在处理您的请求...")
                            .build());

            // 3. 判断问题是否清晰
            boolean isClear = checkQuestionClarity(request.getMessage());

            if (!isClear) {
                // 问题不清晰，请求用户补充
                handleUnclearQuestionStream(session, request.getMessage(), emitter);
            } else {
                // 问题清晰，进入RUNNING阶段
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
     * 检查问题是否清晰
     */
    private boolean checkQuestionClarity(String message) {
        log.info("Checking question clarity for message: {}", message);


        // FIXME: 这里的提示词可以根据实际需求进行调整和优化, 有必要提供上下文信息
        String clarityPrompt = """
            请判断以下用户问题是否足够清晰和具体，能够直接执行研究任务：
        
            用户问题：%s
        
            判断标准：
            1. 问题是否明确具体
            2. 研究范围是否清楚
            3. 期望的结果是否明确
            4. 是否包含足够的上下文信息
        
            回答要求（仅返回 JSON，不要添加其他说明）：
            - isClear：布尔值，true 或 false。
        
            示例：
        
            example1:
            {
                "isClear": true
            }
        
            example2:
            {
                "isClear": false,
            }
        
            你的回答必须只返回 JSON，不要返回任何额外文本或说明。
            """.formatted(message);

        Generation response = chatModel.call(new Prompt(new UserMessage(clarityPrompt))).getResult();
        String text = response.getOutput().getText();
        AssertUtil.isNotNull(text, "AI未返回澄清结果");
        ClarifyResult convert = checkConverter.convert(text);

        log.info("Clarity check result: {}", convert.toString());

        return convert.isClear();
    }

    /**
     * 保存澄清历史
     */
    private void saveChatHistory(String sessionId, String question, String answer, boolean completed) {
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
     * 获取澄清轮数
     */
    private Integer getChatRoundNumber(String sessionId) {
        ChatHistory latestHistory = chatHistoryRepository.findTopBySessionIdOrderByRoundNumberDesc(sessionId);
        return latestHistory != null ? latestHistory.getRoundNumber() : 0;
    }

    /**
     * 流式处理不清晰的问题
     */
    private void handleUnclearQuestionStream(Session session, String message, SseEmitter emitter) {
        log.info("Handling unclear question for sessionId: {}", session.getSessionId());

        // 生成澄清问题
        String clarificationPrompt = """
            用户提出了以下问题，但不够清晰具体。请生成1-2个澄清问题来帮助用户明确研究需求：
            
            用户问题：%s
            
            请生成2-3个简洁明确的澄清问题，帮助用户提供更多必要信息。
            """.formatted(message);

        // 使用流式调用
        Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(new UserMessage(clarificationPrompt)));
        handleStreamingResponse(responseFlux, session, message, emitter);
    }

    /**
     * 流式处理清晰的问题
     */
    private void handleClearQuestionStream(Session session, String message, SseEmitter emitter) {
        log.info("Handling clear question for sessionId: {}", session.getSessionId());

        try {
            // 设置session状态为RUNNING
            session.setStatus(SessionStatus.CLARIFYING);
            sessionRepository.save(session);
            log.info("Updated session status to RUNNING");

            // 发送完成信息
            StreamChatResponse response = StreamChatResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionStatus(SessionStatus.CLARIFYING)
                    .streamFinished(true)
                    .build();

            sseTransportClient.sendEndMessage(emitter, response);

            // TODO: 在这里启动sub-agent执行具体的研究任务
            // startSubAgentStream(session, message, emitter);

        } catch (Exception e) {
            sseTransportClient.handleError(emitter, e);
        }
    }

    // TODO: 预留给sub-agent启动的方法
    private void startSubAgent(Session session, String message) {
        log.info("Starting sub-agent for sessionId: {}", session.getSessionId());
        // 这里将来实现启动具体的研究代理
        // 1. 解析研究任务
        // 2. 选择合适的sub-agent
        // 3. 启动异步执行
    }

    /**
     * 处理流式响应
     */
    private void handleStreamingResponse(Flux<ChatResponse> responseFlux,
                                       Session session, String message, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        responseFlux.subscribe(
            response -> {
                String content = response.getResult().getOutput().toString();
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
    
    // TODO: 预留给流式sub-agent启动的方法
    private void startSubAgentStream(Session session, String message, SseEmitter emitter) {
        log.info("Starting streaming sub-agent for sessionId: {}", session.getSessionId());
        // 这里将来实现启动具体的流式研究代理
        // 1. 解析研究任务
        // 2. 选择合适的sub-agent
        // 3. 启动异步执行并通过SSE发送进度
    }
}