package com.luna.deepluna.dto.request.websearch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TavilyWebSearchRequestBody implements WebSearchRequestBody {
    /** 必填：搜索查询语句 */
    private String query;

    private boolean autoParameters;

    /** 搜索类别：general（默认）、news、finance */
    private String topic;

    /** 搜索深度：basic（1 credit） 或 advanced（2 credits） */
    private String searchDepth;

    /** 每个来源返回的最大内容片段数（仅 advanced 有效），范围 1-3 */
    private Integer chunksPerSource;

    /** 最大返回结果数，范围 0-20，默认 1 */
    private Integer maxResults;

    /** 时间范围过滤：day/d/week/w/month/m/year/y */
    private String days;

    /** 仅 news 类别有效：指定多少天内的结果，x >= 1 */
    private Integer daysBack;

    /** 起始发布日期（格式：YYYY-MM-DD） */
    private LocalDate startDate;

    /** 结束发布日期（格式：YYYY-MM-DD） */
    private LocalDate endDate;

    /** 是否包含 LLM 生成的答案：true/basic/advanced */
    private String includeAnswer;

    /** 是否包含原始网页内容：true/markdown/text */
    private String includeRawContent;

    /** 是否包含图片搜索结果 */
    private Boolean includeImages;

    /** 若 includeImages=true，是否为每张图添加描述 */
    private Boolean includeImageDescriptions;

    /** 是否包含每个结果的 favicon URL */
    private Boolean includeFavicon;

    /** 限定只从这些域名中获取结果（最多 300 个） */
    private List<String> includeDomains;

    /** 排除这些域名的结果（最多 150 个） */
    private List<String> excludeDomains;

    /** 优先展示来自指定国家的内容（仅 topic=general 时有效） */
    private String country;

    public static TavilyWebSearchRequestBody toDefaultWebSearchRequest(String query) {
        return TavilyWebSearchRequestBody.builder()
                .query(query)
                .autoParameters(false)
                .searchDepth("basic")
                .chunksPerSource(3)
                .maxResults(1)
                .includeRawContent("markdown")
                .includeImages(false)
                .build();
    }
}
