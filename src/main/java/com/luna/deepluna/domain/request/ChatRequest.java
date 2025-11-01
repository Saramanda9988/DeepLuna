package com.luna.deepluna.domain.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天请求")
public class ChatRequest {
    @NotNull
    @Schema(description = "聊天内容", example = "你好，今天的天气怎么样？")
    private String message;

    @Schema(description = "使用的模型的id，如果不存在则使用默认", example = "gpt-4")
    private String modelId;

    @NotNull
    @Schema(description = "会话ID", example = "123456789")
    private String sessionId;
}
