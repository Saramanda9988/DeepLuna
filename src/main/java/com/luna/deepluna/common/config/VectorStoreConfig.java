package com.luna.deepluna.common.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Locale;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.pgvector.index-type:HNSW}")
    private String indexType;

    @Value("${spring.ai.vectorstore.pgvector.distance-type:COSINE_DISTANCE}")
    private String distanceType;

    @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
    private String schemaName;

    @Value("${spring.ai.vectorstore.pgvector.vector-table-name:vector_store}")
    private String vectorTableName;

    @Value("${spring.ai.vectorstore.pgvector.max-document-batch-size:10000}")
    private int maxDocumentBatchSize;

    @Bean
    public PgVectorStore pgVectorStore(
            JdbcTemplate jdbcTemplate,
            @Qualifier("zhiPuAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .distanceType(parseDistanceType(distanceType))
                .indexType(parseIndexType(indexType))
                .schemaName(schemaName)
                .vectorTableName(vectorTableName)
                .maxDocumentBatchSize(maxDocumentBatchSize)
                .build();
    }

    private PgVectorStore.PgIndexType parseIndexType(String value) {
        return PgVectorStore.PgIndexType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private PgVectorStore.PgDistanceType parseDistanceType(String value) {
        return PgVectorStore.PgDistanceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
