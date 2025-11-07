package com.luna.deepluna.domain.response;

import com.luna.deepluna.domain.entity.Model;
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

    public static Model toEntity(ModelResponse response) {
        Model model = new Model();
        model.setModelId(response.getModelId());
        model.setName(response.getName());
        model.setUrl(response.getUrl());
        model.setCreateTime(response.getCreateTime());
        return model;
    }
}