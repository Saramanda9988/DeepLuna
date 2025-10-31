package com.luna.deepluna.agent.agentTool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.dto.request.websearch.TavilyWebSearchRequestBody;
import com.luna.deepluna.dto.request.websearch.WebSearchRequestBody;
import com.luna.deepluna.dto.response.websearch.TavilySearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubAgentTools {

    @Value("${websearch.tavily.api.key}")
    public String tavilyApiKey;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    // TODO: 这里是写死的使用Tavily搜索，后续可以支持更多搜索引擎和配置选项
    @Tool(description = "请求网络查询")
    public TavilySearchResponse webSearch(String query) {

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
    public String thinkTool() {
        // TODO: 实现子智能体的反思与策略规划逻辑
        return "";
    }
}
