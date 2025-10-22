package com.luna.deepluna.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建Session请求DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSessionRequest {
    private Long userId;
    private String model;
}