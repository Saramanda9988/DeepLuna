package com.luna.deepluna.dto.response.websearch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TavilySearchResponse {
    private String query;

    private List<SearchResult> results;

    private String responseTime; // 或 Double，根据实际 API 返回类型调整

    private String requestId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SearchResult {
        private String title;

        private String url;

        private String content;

        private Double score;

        private String publishedDate;

        private String favicon;
    }
}
