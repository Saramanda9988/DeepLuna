package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.agent.SubAgent;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class SupervisorTools {

    private final SubAgent subAgent;

    private final DeepSeekChatModel chatModel;

    private final ContextCache contextCache;

    @Tool(description = "将研究任务委派给专业子智能体")
    public String conductResearch(@ToolParam(description = "子智能体的研究主题") String researchTopic) {
        SubAgentContext context = SubAgentContext.builder()
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .build())
                .researchTopic(researchTopic)
                .maxSubReflections(5)
                .status(SubAgentTaskStatus.PENDING)
                .subAgentId(UUID.randomUUID().toString())
                .build();
        contextCache.putSubAgent(context.getSubAgentId(), context);
        return subAgent.startSubAgentResearch(context.getSubAgentId());
    }

    @Tool(description = "表明研究已完成")
    public void researchComplete() {

    }

    @Tool(description = "用于研究过程中的反思与策略规划")
    public String thinkTool() {
        // TODO: 实现监督者智能体的反思与策略规划逻辑
        return "";
    }
}
