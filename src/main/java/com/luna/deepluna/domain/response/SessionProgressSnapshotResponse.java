package com.luna.deepluna.domain.response;

import com.luna.deepluna.common.enums.SessionStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话研究进度快照")
public class SessionProgressSnapshotResponse {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话状态")
    private SessionStatus sessionStatus;

    @Schema(description = "Supervisor状态")
    private SupervisorAgentState supervisorState;

    @Schema(description = "最近一条进度消息")
    private String latestMessage;

    @Schema(description = "最近更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "子智能体总数")
    private Integer totalSubAgents;

    @Schema(description = "运行中的子智能体数量")
    private Integer runningSubAgents;

    @Schema(description = "已完成的子智能体数量")
    private Integer completedSubAgents;

    @Schema(description = "是否已结束")
    private Boolean finished;

    @Schema(description = "子智能体列表")
    private List<SessionProgressSubAgentResponse> subAgents;
}
