package com.luna.deepluna;

import com.luna.deepluna.domain.request.DocumentUploadRequest;
import com.luna.deepluna.domain.response.DocumentUploadResponse;
import com.luna.deepluna.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class EmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private PgVectorStore pgVectorStore;

    @Test
    public void testUploadTextDocument() {
        // 创建测试文档内容
        String content = """
                Spring AI 是一个强大的框架，用于构建 AI 应用程序。
                它提供了与各种 AI 模型的集成，包括 OpenAI、Azure OpenAI、DeepSeek 等。
                Spring AI 还提供了向量存储功能，支持 PostgreSQL 的 pgvector 扩展。
                这使得实现 RAG（检索增强生成）变得非常简单。
                """;

        // 创建模拟文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "spring-ai-intro.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // 创建请求
        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setUserId(1L);
        request.setCategory("tutorial,spring-ai");
        request.setDescription("Spring AI 框架介绍");
        request.setEnableChunking(true);
        request.setChunkSize(100);
        request.setChunkOverlap(20);

        // 执行上传
        DocumentUploadResponse response = embeddingService.uploadAndEmbedDocument(file, request);

        // 验证结果
        log.info("上传结果: {}", response);
        assertEquals("success", response.getStatus());
        assertNotNull(response.getDocumentId());
        assertTrue(response.getVectorCount() > 0);

        // 测试检索
        testSearch(response.getDocumentId());
    }

    @Test
    public void testUploadMarkdownDocument() {
        // 创建Markdown文档
        String content = """
                # Deep Research Agent
                
                ## 概述
                Deep Research Agent 是一个自动化研究工作流系统。
                
                ## 功能特性
                - 多 Agent 协作
                - 向量数据库检索
                - RAG 增强生成
                - WebSocket 实时通信
                
                ## 技术栈
                - Spring Boot 3.2.5
                - Spring AI
                - PostgreSQL + pgvector
                - DeepSeek API
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "README.md",
                "text/markdown",
                content.getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setUserId(2L);
        request.setCategory("documentation");
        request.setDescription("项目说明文档");
        request.setEnableChunking(false);

        DocumentUploadResponse response = embeddingService.uploadAndEmbedDocument(file, request);

        log.info("Markdown上传结果: {}", response);
        assertEquals("success", response.getStatus());
        assertNotNull(response.getDocumentId());
    }

    private void testSearch(String documentId) {
        // 搜索相关文档
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Spring AI 的向量存储功能")
                .topK(3)
                .similarityThreshold(0.5)
                .filterExpression("document_id == '" + documentId + "'")
                .build();

        List<Document> results = pgVectorStore.similaritySearch(searchRequest);

        log.info("检索到 {} 个相关文档片段", results.size());
        for (Document doc : results) {
            log.info("相似度: {}, 内容: {}", doc.getScore(), doc.getFormattedContent());
            log.info("元数据: {}", doc.getMetadata());
        }

        // 验证检索结果
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getScore() >= 0.5);
    }

    @Test
    public void testSearchByCategory() {
        // 按分类搜索
        SearchRequest searchRequest = SearchRequest.builder()
                .query("AI框架")
                .topK(5)
                .filterExpression("category == 'tutorial,spring-ai'")
                .build();

        List<Document> results = pgVectorStore.similaritySearch(searchRequest);
        
        log.info("按分类检索到 {} 个文档", results.size());
        for (Document doc : results) {
            log.info("分类: {}, 文件名: {}", 
                    doc.getMetadata().get("category"), 
                    doc.getMetadata().get("file_name"));
        }
    }

    @Test
    public void testSearchByUserId() {
        // 按用户ID搜索
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Spring AI")
                .topK(10)
                .filterExpression("user_id == '1'")
                .build();

        List<Document> results = pgVectorStore.similaritySearch(searchRequest);
        
        log.info("用户1的文档数量: {}", results.size());
        results.forEach(doc -> {
            log.info("用户文档: {} - {}", 
                    doc.getMetadata().get("file_name"),
                    doc.getFormattedContent().substring(0, Math.min(50, doc.getFormattedContent().length())));
        });
    }

    @Test
    public void testInvalidFile() {
        // 测试空文件
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setUserId(1L);

        DocumentUploadResponse response = embeddingService.uploadAndEmbedDocument(emptyFile, request);
        
        log.info("空文件上传结果: {}", response);
        assertEquals("failed", response.getStatus());
    }

    @Test
    public void testUnsupportedFileType() {
        // 测试不支持的文件类型
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "fake content".getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setUserId(1L);

        DocumentUploadResponse response = embeddingService.uploadAndEmbedDocument(file, request);
        
        log.info("不支持文件类型上传结果: {}", response);
        assertEquals("failed", response.getStatus());
        assertTrue(response.getMessage().contains("不支持的文件类型"));
    }
}
