package com.luna.deepluna.domain.response;

import com.luna.deepluna.common.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session详情响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionDetailResponse {
    private String sessionId;
    private Long userId;
    private String model;
    private String summary;
    private SessionStatus status;
    private String researchBrief;
    private LocalDateTime createdTime;
    private LocalDateTime updateTime;
}