package com.luna.deepluna.dto.response;

import com.luna.deepluna.common.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天响应基类")
public class ChatResp {
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "响应消息")
    private String message;
    
    @Schema(description = "会话状态")
    private SessionStatus sessionStatus;
}