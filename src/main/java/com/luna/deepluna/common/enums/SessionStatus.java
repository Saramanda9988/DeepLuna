package com.luna.deepluna.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SessionStatus {
    INIT(0, "初始化"),
    CLARIFYING(1, "澄清中"),
    RUNNING(2, "运行中"),
    REPORTING(3, "报告中"),
    COMPLETED(4, "已完成"),
    FAILED(5, "失败");

    private final int code;
    private final String description;
}
