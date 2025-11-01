package com.luna.deepluna.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {
    private String sessionId;
    private String summary;
}