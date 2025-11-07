package com.luna.deepluna.agent;

import com.luna.deepluna.agent.agentTool.SubAgentTools;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.cache.ContextCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubAgent {

    private final SubAgentTools subAgentTools;

    private final ChatClientCache chatClientCache;

    private final ToolCallingManager toolCallingManager;

    private final ContextCache contextCache;

    public String startSubAgentResearch(String researchTopic) {
        SubAgentContext context = SubAgentContext.builder()
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .build())
                .researchTopic(researchTopic)
                .maxWebSearch(5)
                .status(SubAgentTaskStatus.PENDING)
                .subAgentId(UUID.randomUUID().toString())
                .build();
        contextCache.putSubAgent(context.getSubAgentId(), context);
        return subAgent(context.getSubAgentId());
    }

    private String subAgent(String subAgentId) {
        SubAgentContext subAgent = contextCache.getSubAgent(subAgentId);
        AssertUtil.isNotNull(subAgent, "Sub Agent not found: subAgentId=" + subAgentId);

        OpenAiChatModel chatModel = chatClientCache.getChatClient(subAgentId);
        AssertUtil.isNotNull(chatModel, "Chat model not found for Sub Agent: subAgentId=" + subAgentId);

        ChatMemory chatMemory = subAgent.getChatMemory();

        chatMemory.add(subAgentId, new AssistantMessage(Prompts.SUB_AGENT_PROMPT.formatted(LocalDateTime.now())));
        chatMemory.add(subAgentId, new UserMessage("Research Topic" + subAgent.getResearchTopic()));
        subAgent.setStatus(SubAgentTaskStatus.IN_PROGRESS);
        log.info("Sub Agent started: subAgentId={}", subAgentId);

        Map<String, Object> webSearchUsage = new HashMap<>();
        webSearchUsage.put("count", new AtomicInteger(0));
        webSearchUsage.put("max", subAgent.getMaxWebSearch());

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(subAgentTools))
                .toolContext(webSearchUsage)
                .internalToolExecutionEnabled(false)
                .build();

        Prompt promptWithMemory = new Prompt(chatMemory.get(subAgentId), chatOptions);

        ChatResponse response = chatModel.call(promptWithMemory);
        while (response.hasToolCalls()) {
            Generation result = response.getResult();
            chatMemory.add(subAgentId, result.getOutput());
            List<AssistantMessage.ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();

            // 统计 webSearch 工具调用次数
            log.info("Sub Agent executing tool calls: subAgentId={}, toolCalls={}", subAgentId, toolCalls);

            ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
            log.info("Sub Agent received tool execution result: subAgentId={}, toolResults={}",
                    subAgentId, executionResult.conversationHistory().getLast());
            chatMemory.add(subAgentId, executionResult.conversationHistory().getLast());

            promptWithMemory = new Prompt(chatMemory.get(subAgentId), chatOptions);
            response = chatModel.call(promptWithMemory);
        }

        // 最终响应处理
        chatMemory.add(subAgentId,
                new AssistantMessage(Prompts.COMPRESS_RESEARCH_SYSTEM_PROMPT.formatted(LocalDateTime.now())));
        log.info("Sub Agent starting compression: subAgentId={}", subAgentId);
        Generation result = chatModel.call(new Prompt(chatMemory.get(subAgentId))).getResult();
        String compressResp = result.getOutput().getText();
        AssertUtil.isFalse(compressResp == null || compressResp.isEmpty(), "压缩结果为空");
        subAgent.setStatus(SubAgentTaskStatus.COMPLETED);
        log.info("Sub Agent completed: subAgentId={}", subAgentId);
        return compressResp;
    }
}
