package com.luna.deepluna.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建/更新Model请求DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelRequest {
    private String name;
    private String token;
    private String url;
}