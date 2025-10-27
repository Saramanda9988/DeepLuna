package com.luna.deepluna.controller;


import com.luna.deepluna.common.client.SseTransportClient;
import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.dto.request.ChatRequest;
import com.luna.deepluna.dto.response.ChatResp;
import com.luna.deepluna.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
@Tag(name = "ChatController", description = "聊天控制器")
public class ChatController {
    
    private final ChatService chatService;

    private final SseTransportClient sseTransportClient;
    
    /**
     * 聊天接口 - 流式响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天接口", description = "处理用户聊天消息，支持流式响应、问题澄清和研究任务执行")
    public SseEmitter chatStream(@RequestBody @Validated ChatRequest request) {
        log.info("Received streaming chat request: sessionId={}, message={}", request.getSessionId(), request.getMessage());

        SseEmitter sseEmitter = sseTransportClient.createConnection(30000L);

        chatService.processChatStream(request, sseEmitter);
        
        return sseEmitter;
    }
}
