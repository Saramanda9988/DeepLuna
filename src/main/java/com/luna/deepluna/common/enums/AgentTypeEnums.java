package com.luna.deepluna.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AgentTypeEnums {
    CLARIFICATION(1, "让用户进行澄清"),
    BRIEF(2, "生成简报"),
    RESEARCH(3, "进行研究"),
    SUMMARIZE(4, "总结信息"),
    REPORT(5, "生成报告");

    private final int type;
    private final String description;
}
