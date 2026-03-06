package com.luna.deepluna.domain.response.websearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchResponse {

    private String provider;
    private String query;
    private Double responseTime;
    private String requestId;
    private List<SearchResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String title;
        private String url;
        private String content;
        private Double score;
        private String publishedDate;
        private String favicon;
    }
}

