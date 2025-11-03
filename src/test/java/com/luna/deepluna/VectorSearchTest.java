package com.luna.deepluna;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
public class VectorSearchTest {
    @Autowired
    private PgVectorStore pgVectorStore;

    @Test
    public void testStoreSearch() {
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));

        // Add the documents to PGVector
        pgVectorStore.add(documents);

        // Retrieve documents similar to a query
        List<Document> results = this.pgVectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        for (Document doc : results) {
            log.info("Found document: {} with metadata: {} with score: {}", doc.getText(), doc.getMetadata(), doc.getScore());
        }
    }
}
