package com.luna.deepluna.agent.agentTool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.request.websearch.TavilyWebSearchRequestBody;
import com.luna.deepluna.domain.request.websearch.WebSearchRequestBody;
import com.luna.deepluna.domain.response.websearch.TavilySearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubAgentTools {

    @Value("${websearch.tavily.api.key}")
    public String tavilyApiKey;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    // TODO: 这里是写死的使用Tavily搜索，后续可以支持更多搜索引擎和配置选项
    @Tool(description = "请求网络查询")
    public TavilySearchResponse webSearch(String query, ToolContext toolContext) {
        log.info("SubAgentTools#webSearch called with query: {}", query);
        AtomicInteger count = (AtomicInteger) toolContext.getContext().get("count");
        Integer maxUsage = (Integer) toolContext.getContext().get("max");

        if (count.incrementAndGet() > maxUsage) {
            throw new RuntimeException("webSearch#已达到最大使用次数限制: " + maxUsage);
        }

        WebSearchRequestBody body = TavilyWebSearchRequestBody.toDefaultWebSearchRequest(query);
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
            return objectMapper.readValue(response.body(), TavilySearchResponse.class);
        } catch (InterruptedException | IOException e) {
            throw new BusinessException("webSearch#网络请求失败: " + e.getMessage());
        }
    }

    @Tool(description = "用于研究过程中的反思与策略规划")
    public String thinkTool(@ToolParam(description = "智能体的反思内容") String reflectionInput) {
        return "[Reflection Result] " + reflectionInput;
    }
}
