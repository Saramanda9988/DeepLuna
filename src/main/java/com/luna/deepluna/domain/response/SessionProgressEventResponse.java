package com.luna.deepluna.domain.response;

import com.luna.deepluna.common.enums.SessionProgressEventType;
import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话研究进度事件")
public class SessionProgressEventResponse {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "事件类型")
    private SessionProgressEventType eventType;

    @Schema(description = "事件消息")
    private String message;

    @Schema(description = "会话状态")
    private SessionStatus sessionStatus;

    @Schema(description = "Supervisor状态")
    private SupervisorAgentState supervisorState;

    @Schema(description = "当前相关子任务")
    private SessionProgressSubAgentResponse subAgent;

    @Schema(description = "是否已结束")
    private Boolean finished;

    @Schema(description = "事件时间")
    private LocalDateTime timestamp;

    @Schema(description = "事件触发后的完整快照")
    private SessionProgressSnapshotResponse snapshot;
}
