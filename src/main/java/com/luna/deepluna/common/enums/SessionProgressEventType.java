package com.luna.deepluna.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SessionProgressEventType {
    SNAPSHOT("当前进度快照"),
    SESSION_STATUS_CHANGED("会话状态变更"),
    SUPERVISOR_STATUS_CHANGED("主管智能体状态变更"),
    SUB_AGENT_CREATED("子智能体已创建"),
    SUB_AGENT_STATUS_CHANGED("子智能体状态变更"),
    RESEARCH_BRIEF_GENERATED("研究简报已生成"),
    MESSAGE("进度消息");

    private final String description;
}
