package com.luna.deepluna.common.client;

import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.response.ChatResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@Component
public class SseTransportClient implements MessageTransportClient<SseEmitter> {
    
    /**
     * 创建连接
     *
     * @param timeout 超时时间(毫秒)
     * @return 连接对象
     */
    @Override
    public SseEmitter createConnection(long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);
        
        // 设置连接完成和错误回调
        emitter.onCompletion(() -> log.info("SSE connection completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout");
            emitter.complete();
        });
        emitter.onError(throwable -> {
            log.error("SSE connection error", throwable);
            emitter.completeWithError(throwable);
        });
        
        return emitter;
    }

    /**
     * 发送消息
     *
     * @param connection         连接对象
     * @param streamChatResponse 消息内容
     */
    @Override
    public void sendMessage(SseEmitter connection, ChatResp streamChatResponse) {
        try {
            connection.send(SseEmitter.event()
                .name("response")
                .data(streamChatResponse));
        } catch (Exception e) {
            log.error("Error sending SSE message", e);
            handleError(connection, e);
        }
    }

    /**
     * 发送结束消息
     *
     * @param connection         连接对象
     * @param streamChatResponse 消息内容
     */
    @Override
    public void sendEndMessage(SseEmitter connection, ChatResp streamChatResponse) {
        try {
            connection.send(SseEmitter.event()
                .name("end")
                .data(streamChatResponse));
            completeConnection(connection);
        } catch (Exception e) {
            log.error("Error sending SSE end message", e);
            handleError(connection, e);
        }
    }

    /**
     * 完成连接
     *
     * @param connection 连接对象
     */
    @Override
    public void completeConnection(SseEmitter connection) {
        try {
            connection.complete();
            log.info("SSE connection completed successfully");
        } catch (Exception e) {
            log.error("Error completing SSE connection", e);
            connection.completeWithError(e);
        }
    }

    /**
     * 处理错误
     *
     * @param connection 连接对象
     * @param error      错误对象
     */
    @Override
    public void handleError(SseEmitter connection, Throwable error) {
        try {
            if (error instanceof BusinessException be) {
                log.error("Handling SSE error", be);
                connection.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "code", be.getErrorCode(),
                                "message", "处理请求时发生错误: " + be.getErrorMsg()
                        )));
                connection.completeWithError(error);
            } else {
                log.error("Handling SSE error", error);
                connection.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "code", -1001,
                                "message", "处理请求时发生错误: " + error.getMessage()
                        )));
                connection.completeWithError(error);
            }
        } catch (Exception e) {
            log.error("Error handling SSE error", e);
        }
    }
}
