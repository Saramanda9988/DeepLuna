package com.luna.deepluna.agent.agentTool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubAgentTools {
    @Tool(description = "表明研究已完成")
    public Map<String, String> webSearch() {
        // TODO: 实现子智能体的研究完成逻辑
        return Map.of();
    }

    @Tool(description = "用于研究过程中的反思与策略规划")
    public String thinkTool() {
        // TODO: 实现子智能体的反思与策略规划逻辑
        return "";
    }
}
