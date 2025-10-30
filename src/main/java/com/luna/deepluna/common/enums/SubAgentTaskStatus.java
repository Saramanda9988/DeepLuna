package com.luna.deepluna.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of possible task execution statuses.
 */
@AllArgsConstructor
@Getter
public enum SubAgentTaskStatus {
    PENDING(1, "任务已创建，等待执行"),
    IN_PROGRESS(2, "任务正在执行"),
    COMPLETED(3, "任务已完成"),
    FAILED(4, "任务执行失败");

    private final int status;
    private final String description;
}