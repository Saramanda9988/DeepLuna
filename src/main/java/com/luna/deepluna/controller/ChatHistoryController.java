package com.luna.deepluna.controller;

import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.domain.response.ChatHistoryResponse;
import com.luna.deepluna.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/capi/chat-history")
@RequiredArgsConstructor
@Tag(name = "ChatHistoryController", description = "聊天历史查询接口")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    /**
     * 查询指定会话聊天历史
     */
    @GetMapping("/list/{sessionId}")
    @Operation(summary = "查询会话聊天历史")
    public ApiResult<List<ChatHistoryResponse>> getSessionChatHistory(@PathVariable String sessionId) {
        return ApiResult.success(chatHistoryService.getSessionChatHistory(sessionId));
    }
}
