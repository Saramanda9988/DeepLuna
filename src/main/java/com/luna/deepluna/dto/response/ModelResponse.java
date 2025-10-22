package com.luna.deepluna.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model响应DTO（不包含token）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelResponse {
    private String modelId;
    private String name;
    private String url;
    private Instant createTime;
}