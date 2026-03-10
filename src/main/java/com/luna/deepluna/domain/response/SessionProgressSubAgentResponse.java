package com.luna.deepluna.domain.response;

import com.luna.deepluna.common.enums.SubAgentTaskStatus;
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
@Schema(description = "会话研究进度中的子任务信息")
public class SessionProgressSubAgentResponse {

    @Schema(description = "子智能体ID")
    private String subAgentId;

    @Schema(description = "研究主题")
    private String researchTopic;

    @Schema(description = "子智能体状态")
    private SubAgentTaskStatus status;

    @Schema(description = "最近更新时间")
    private LocalDateTime updatedAt;
}
