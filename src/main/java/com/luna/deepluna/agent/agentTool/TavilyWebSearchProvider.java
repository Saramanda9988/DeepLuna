package com.luna.deepluna.agent.agentTool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.request.websearch.TavilyWebSearchRequestBody;
import com.luna.deepluna.domain.request.websearch.WebSearchRequestBody;
import com.luna.deepluna.domain.response.websearch.TavilySearchResponse;
import com.luna.deepluna.domain.response.websearch.WebSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class TavilyWebSearchProvider implements WebSearchProvider {

    @Value("${websearch.tavily.api.key}")
    private String tavilyApiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public String providerId() {
        return "tavily";
    }

    @Override
    public WebSearchResponse search(String query) {
        WebSearchRequestBody body = TavilyWebSearchRequestBody.toDefaultWebSearchRequest(query);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException("webSearch#请求参数序列化失败: " + e.getMessage());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tavily.com/search"))
                .header("Authorization", "Bearer " + tavilyApiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException("webSearch#网络请求失败，状态码: " + response.statusCode() + ", 错误信息: " + response.body());
            }
            TavilySearchResponse tavily = objectMapper.readValue(response.body(), TavilySearchResponse.class);
            return toWebSearchResponse(tavily);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("webSearch#请求被中断: " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException("webSearch#网络请求失败: " + e.getMessage());
        }
    }

    private WebSearchResponse toWebSearchResponse(TavilySearchResponse tavily) {
        return WebSearchResponse.builder()
                .provider(providerId())
                .query(tavily.getQuery())
                .requestId(tavily.getRequestId())
                .responseTime(parseResponseTime(tavily.getResponseTime()))
                .results(tavily.getResults() == null ? null :
                        tavily.getResults().stream().map(result -> WebSearchResponse.SearchResult.builder()
                                .title(result.getTitle())
                                .url(result.getUrl())
                                .content(result.getContent())
                                .score(result.getScore())
                                .publishedDate(result.getPublishedDate())
                                .favicon(result.getFavicon())
                                .build()).toList())
                .build();
    }

    private Double parseResponseTime(String responseTime) {
        if (responseTime == null || responseTime.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(responseTime);
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse Tavily response time: {}", responseTime);
            return null;
        }
    }
}

