package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.agent.SubAgent;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupervisorTools {

    private final SubAgent subAgent;

    private final DeepSeekChatModel chatModel;

    private final ContextCache contextCache;

    @Tool(description = "将研究任务委派给专业子智能体")
    public String conductResearch(@ToolParam(description = "子智能体的研究主题") String researchTopic, ToolContext toolContext) {
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
