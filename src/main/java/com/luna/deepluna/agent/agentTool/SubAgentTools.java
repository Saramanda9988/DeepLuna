package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.domain.response.rag.RagResponse;
import com.luna.deepluna.domain.response.websearch.WebSearchResponse;
import com.luna.deepluna.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubAgentTools {

    private final PgVectorStore pgVectorStore;
    private final WebSearchProviderRegistry webSearchProviderRegistry;
    private final AppConfigService appConfigService;

    @Tool(description = "请求网络查询")
    public WebSearchResponse webSearch(String query, ToolContext toolContext) {
        log.info("SubAgentTools#webSearch called with query: {}", query);
        AtomicInteger count = (AtomicInteger) toolContext.getContext().get("count");
        Integer maxUsage = (Integer) toolContext.getContext().get("max");

        if (count.incrementAndGet() > maxUsage) {
            throw new RuntimeException("webSearch#已达到最大使用次数限制: " + maxUsage);
        }
        String providerId = appConfigService.getWebSearchProvider();
        return webSearchProviderRegistry.getProvider(providerId).search(query);
    }

    @Tool(description = "用于研究过程中的反思与策略规划")
    public String thinkTool(@ToolParam(description = "智能体的反思内容") String reflectionInput) {
        return "[Reflection Result] " + reflectionInput;
    }

    @Tool(description = "用于从预先索引的文档中检索相关信息")
    public List<RagResponse> ragTool(@ToolParam(description = "需要通过rag检索的内容") String query, ToolContext toolContext) {
        log.info("SubAgentTools#ragTool called with query: {}", query);
        AtomicInteger count = (AtomicInteger) toolContext.getContext().get("count");
        Integer maxUsage = (Integer) toolContext.getContext().get("max");

        if (count.incrementAndGet() > maxUsage) {
            throw new RuntimeException("ragTool#已达到最大使用次数限制: " + maxUsage);
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();
        List<Document> results = pgVectorStore.similaritySearch(searchRequest);
        return results.stream().map(RagResponse::fromDocument).toList();
    }
}
