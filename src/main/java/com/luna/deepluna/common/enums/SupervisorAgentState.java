package com.luna.deepluna.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SupervisorAgentState {

    IDLE(0, "空闲中"),
    INITIALIZING(1, "初始化任务"),
    RUNNING(3, "拆分任务并分发"),
    COMPLETED(5, "完成"),
    FAILED(6, "失败")
    ;

    private final int type;
    private final String description;
}
