package com.luna.deepluna.agent;

import com.luna.deepluna.agent.agentTool.SubAgentTools;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubAgent {

    private final SubAgentTools subAgentTools;

    private final DeepSeekChatModel chatModel;

    private final ToolCallingManager toolCallingManager;

    private final ContextCache contextCache;

    public String startSubAgentResearch(String subAgentId) {
        SubAgentContext subAgent = contextCache.getSubAgent(subAgentId);
        ChatMemory chatMemory = subAgent.getChatMemory();
        chatMemory.add(subAgentId, new AssistantMessage(Prompts.SUB_AGENT_PROMPT.formatted(LocalDateTime.now())));
        subAgent.setStatus(SubAgentTaskStatus.IN_PROGRESS);
        log.info("Sub Agent started: subAgentId={}", subAgentId);

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(subAgentTools))
                .internalToolExecutionEnabled(false)
                .build();

        Prompt promptWithMemory = new Prompt(chatMemory.get(subAgentId), chatOptions);

        ChatResponse response = chatModel.call(promptWithMemory);
        while (response.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolCalls = response.getResult()
                    .getOutput()
                    .getToolCalls();

            ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
            chatMemory.add(subAgentId, executionResult.conversationHistory().getLast());

            promptWithMemory = new Prompt(chatMemory.get(subAgentId), chatOptions);
            response = chatModel.call(promptWithMemory);
        }

        // 最终响应处理
        chatMemory.add(subAgentId,
                new AssistantMessage(Prompts.COMPRESS_RESEARCH_SYSTEM_PROMPT.formatted(LocalDateTime.now())));
        Generation result = chatModel.call(new Prompt(chatMemory.get(subAgentId))).getResult();
        String compressResp = result.getOutput().getText();
        AssertUtil.isFalse(compressResp == null || compressResp.isEmpty(), "压缩结果为空");
        subAgent.setStatus(SubAgentTaskStatus.COMPLETED);
        log.info("Sub Agent completed: subAgentId={}", subAgentId);
        return compressResp;
    }
}
