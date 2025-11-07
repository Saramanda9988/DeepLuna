package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.agent.SubAgent;
import com.luna.deepluna.cache.ContextCache;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupervisorTools {

    private final SubAgent subAgent;

    @Tool(description = "将研究任务委派给专业子智能体")
    public String conductResearch(@ToolParam(description = "子智能体的研究主题") String researchTopic) {
        return subAgent.startSubAgentResearch(researchTopic);
    }

    @Tool(description = "表明研究已完成")
    public void researchComplete() {

    }

    @Tool(description = "用于研究过程中的反思与策略规划")
    public String thinkTool(@ToolParam(description = "监督者智能体的反思内容") String reflectionInput) {
        return "[Reflection Result] " + reflectionInput;
    }
}
