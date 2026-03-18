package com.luna.deepluna.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天历史响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryResponse {
    private String id;
    private String sessionId;
    private String question;
    private String answer;
    private Integer roundNumber;
    private Boolean completed;
    private LocalDateTime createdTime;
}
