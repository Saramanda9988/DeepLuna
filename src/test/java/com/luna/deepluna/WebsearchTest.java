package com.luna.deepluna;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.deepluna.agent.agentTool.SubAgentTools;
import com.luna.deepluna.agent.agentTool.SupervisorTools;
import com.luna.deepluna.agent.context.SubAgentContext;
import com.luna.deepluna.agent.context.SupervisorAgentContext;
import com.luna.deepluna.cache.ChatClientCache;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import com.luna.deepluna.common.enums.SupervisorAgentState;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.prompt.Prompts;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.Model;
import com.luna.deepluna.domain.request.websearch.TavilyWebSearchRequestBody;
import com.luna.deepluna.domain.request.websearch.WebSearchRequestBody;
import com.luna.deepluna.domain.response.websearch.TavilySearchResponse;
import com.luna.deepluna.service.factory.CustomModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Slf4j
public class WebsearchTest {

    @Value("${websearch.tavily.api.key}")
    public String tavilyApiKey;

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Autowired
    HttpClient httpClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SupervisorTools supervisorTools;

    @Autowired
    SubAgentTools subAgentTools;

//    @Autowired
//    DeepSeekChatModel chatModel;

    @Autowired
    ToolCallingManager toolCallingManager;

    @Autowired
    ChatClientCache chatClientCache;

    @Autowired
    CustomModelFactory customModelFactory;

    private void initChatModel(String sessionId) {
        AssertUtil.isNotEmpty(deepseekApiKey, "deepseek.api-key 未配置，无法运行 WebsearchTest");
        Model model = Model.builder()
                .modelId("model-test-001")
                .name("deepseek-chat")
                .token(deepseekApiKey)
                .url("https://api.deepseek.com")
                .build();
        chatClientCache.putBySessionId(sessionId, customModelFactory.createChatModelClient(model));
    }

    @Test
    public void webSearch() {

        WebSearchRequestBody body = TavilyWebSearchRequestBody.toDefaultWebSearchRequest("人工智能的发展前景");
        String s;
        try {
            s = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException("webSearch#请求参数序列化失败: " + e.getMessage());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tavily.com/search"))
                .header("Authorization", "Bearer " + tavilyApiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(10)) // 设置超时时间为10分钟
                .POST(HttpRequest.BodyPublishers.ofString(s))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException("webSearch#网络请求失败，状态码: " + response.statusCode() + ", 错误信息: " + response.body());
            }
            TavilySearchResponse tavilySearchResponse = objectMapper.readValue(response.body(), TavilySearchResponse.class);
            System.out.println(tavilySearchResponse.toString());
        } catch (InterruptedException | IOException e) {
            throw new BusinessException("webSearch#网络请求失败: " + e.getMessage());
        }
    }

    @Test
    public void testSupervisorAgent() {
        SupervisorAgentContext supervisorAgentContext = SupervisorAgentContext.builder()
                .sessionId("test-session-001")
                .maxSubAgentsNumber(5)
                .supervisorId("supervisor-001")
                .status(SupervisorAgentState.INITIALIZING)
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .build())
                .researchBrief("研究人工智能的发展前景")
                .build();

        initChatModel(supervisorAgentContext.getSessionId());
        OpenAiChatModel chatModel = chatClientCache.getBySessionId(supervisorAgentContext.getSessionId());

        String supervisorId = supervisorAgentContext.getSupervisorId();
        ChatMemory chatMemory = supervisorAgentContext.getChatMemory();
        chatMemory.add(supervisorId, new AssistantMessage(Prompts.SUPERVISOR_PROMPT.formatted(
                LocalDateTime.now(),
                5,
                supervisorAgentContext.getMaxSubAgentsNumber()
        )));


        chatMemory.add(supervisorId, new UserMessage("Research Brief:" + """
                # 拉康精神分析历史研究简报\\n\\n本简报概述雅克·拉康精神分析理论的发展历程，重点关注其核心概念演变与当代应用价值。\\n\\n## 核心要点\\n- 从镜像阶段到三界理论的完整发展脉络\\n- 与弗洛伊德传统的继承与断裂关系\\n- 巴黎弗洛伊德学派创立及机构发展史\\n- 临床实践方法（如短会谈）的形成过程\\n- 对当代文化理论与心理治疗的持续影响\\n\\n## 当前趋势\\n拉康理论在数字主体性、后现代身份认同研究中重新获得关注\\n\\n## 交付成果\\n完整的历史发展时间线图（PDF）+ 核心概念演变分析报告\\n\\n## 决策价值\\n理解拉康思想能为组织提供深层心理动力分析工具，适用于文化研究、领导力发展等领域。
                """));
        log.info("Supervisor Agent started: supervisorId={}, sessionId={}",
                supervisorId, supervisorAgentContext.getSessionId());
        // 设置状态为运行中
        supervisorAgentContext.setStatus(SupervisorAgentState.RUNNING);

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(supervisorTools))
                .internalToolExecutionEnabled(false)
                .build();

        Prompt promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);

        log.info("call");
        ChatResponse response = chatModel.call(promptWithMemory);
        // 循环处理工具调用与思考，完成了会退出
        while (response.hasToolCalls()) {
            Generation result = response.getResult();
            // 记录调用工具信息到对话记忆
            log.info("Supervisor Agent received tool calls: supervisorId={}, sessionId={}, toolCalls={}",
                    supervisorId, supervisorAgentContext.getSessionId(), result.getOutput().getToolCalls());
            chatMemory.add(supervisorId, response.getResult().getOutput());
            List<AssistantMessage.ToolCall> toolCalls = result.getOutput().getToolCalls();
            List<AssistantMessage.ToolCall> researchComplete = toolCalls.stream().filter(toolCall -> toolCall.name().equals("researchComplete")).toList();
            boolean isThink = toolCalls.stream().filter(toolCall -> toolCall.name().equals("thinkTool")).count() > 0;
            if (!researchComplete.isEmpty() || toolCalls.isEmpty()) {
                log.info("Research completed by Supervisor Agent: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                supervisorAgentContext.setStatus(SupervisorAgentState.COMPLETED);
                break;
            }

            long conductResearch = toolCalls.stream().filter(tc -> tc.name().equals("conductResearch")).count();

            if (conductResearch > supervisorAgentContext.getMaxSubAgentsNumber()) {
                log.warn("Reached max sub-agents limit: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                chatMemory.add(supervisorId, new UserMessage("一次启动的数量过多，请减少一次启动的子智能体数量。"));
            } else if (conductResearch > 0 && isThink) {
                log.warn("Cannot conduct research and think at the same time: supervisorId={}, sessionId={}",
                        supervisorId, supervisorAgentContext.getSessionId());
                chatMemory.add(supervisorId, new UserMessage("不能同时进行研究和反思，请选择其一。"));
            } else {
                ToolExecutionResult executionResult = toolCallingManager.executeToolCalls(promptWithMemory, response);
                Message message = executionResult.conversationHistory().getLast();
                chatMemory.add(supervisorId, message);
                if (!isThink) {
                    // 如果不是思考，则把子任务的结果加入到笔记中,方便最后进行总结
                    supervisorAgentContext.getNotes().add(message.getText());
                }
            }

            promptWithMemory = new Prompt(chatMemory.get(supervisorId), chatOptions);
            log.info("call again");
            response = chatModel.call(promptWithMemory);
        }
    }

    @Test
    public void startSubAgentResearch() {
        SubAgentContext subAgent = SubAgentContext.builder()
                .sessionId("test-session-001")
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .build())
                .researchTopic("人工智能的发展前景")
                .maxWebSearch(1)
                .status(SubAgentTaskStatus.PENDING)
                .subAgentId("sub-agent-001")
                .build();

        initChatModel(subAgent.getSessionId());
        OpenAiChatModel chatModel = chatClientCache.getBySessionId(subAgent.getSessionId());

        String subAgentId = subAgent.getSubAgentId();
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
            log.info("Sub Agent received tool calls: subAgentId={}, toolCalls={}", subAgentId, result.getOutput().getToolCalls());
            chatMemory.add(subAgentId, result.getOutput());

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
        Generation result = chatModel.call(new Prompt(chatMemory.get(subAgentId))).getResult();
        String compressResp = result.getOutput().getText();
        AssertUtil.isFalse(compressResp == null || compressResp.isEmpty(), "压缩结果为空");
        subAgent.setStatus(SubAgentTaskStatus.COMPLETED);
        log.info("Sub Agent completed: subAgentId={}", subAgentId);
        System.out.println(compressResp);
    }
}
