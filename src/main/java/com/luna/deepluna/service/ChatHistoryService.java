package com.luna.deepluna.service;

import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.ChatHistory;
import com.luna.deepluna.domain.response.ChatHistoryResponse;
import com.luna.deepluna.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 按会话查询聊天历史（按轮次升序）
     */
    public List<ChatHistoryResponse> getSessionChatHistory(String sessionId) {
        AssertUtil.isNotEmpty(sessionId, "SessionId不能为空");
        return chatHistoryRepository.findBySessionIdOrderByRoundNumber(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ChatHistoryResponse toResponse(ChatHistory history) {
        return new ChatHistoryResponse(
                history.getId(),
                history.getSessionId(),
                history.getQuestion(),
                history.getAnswer(),
                history.getRoundNumber(),
                history.getCompleted(),
                history.getCreatedTime()
        );
    }
}
