package com.luna.deepluna.service;

import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.DocumentMetadata;
import com.luna.deepluna.domain.request.DocumentUploadRequest;
import com.luna.deepluna.domain.response.DocumentUploadResponse;
import com.luna.deepluna.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final PgVectorStore pgVectorStore;
    private final DocumentMetadataRepository documentMetadataRepository;

    /**
     * 上传文档并进行向量化存储
     *
     * @param file    上传的文件
     * @param request 请求参数
     * @return 上传响应
     */
    @Transactional
    public DocumentUploadResponse uploadAndEmbedDocument(MultipartFile file, DocumentUploadRequest request) {
        log.info("开始处理文档: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        String documentId = UUID.randomUUID().toString();
        
        try {
            // 1. 验证文件
            validateFile(file);

            // 2. 解析文档
            List<Document> documents = parseDocument(file);
            log.info("文档解析完成，共 {} 个文档片段", documents.size());

            // 3. 添加元数据
            enrichDocumentsWithMetadata(documents, file, request, documentId);

            // 4. 文档分块（如果启用）
            List<Document> processedDocuments = documents;
            if (request.getEnableChunking() != null && request.getEnableChunking()) {
                processedDocuments = chunkDocuments(documents, request);
                log.info("文档分块完成，共 {} 个文档块", processedDocuments.size());
            }

            // 5. 存储到向量数据库
            pgVectorStore.add(processedDocuments);
            log.info("文档向量化并存储成功，共 {} 个向量", processedDocuments.size());

            // 6. 保存文档元数据到数据库
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .documentId(documentId)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .userId(request.getUserId())
                    .category(request.getCategory())
                    .description(request.getDescription())
                    .vectorCount(processedDocuments.size())
                    .status("success")
                    .uploadTime(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            documentMetadataRepository.save(metadata);
            log.info("文档元数据已保存到数据库");

            // 7. 构建响应
            return DocumentUploadResponse.builder()
                    .documentId(documentId)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .vectorCount(processedDocuments.size())
                    .status("success")
                    .message("文档上传并向量化成功")
                    .build();

        } catch (Exception e) {
            log.error("文档处理失败: {}", e.getMessage(), e);

            DocumentMetadata metadata = DocumentMetadata.builder()
                    .documentId(documentId)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .userId(request.getUserId())
                    .category(request.getCategory())
                    .description(request.getDescription())
                    .vectorCount(0)
                    .status("failed")
                    .uploadTime(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            documentMetadataRepository.save(metadata);
            
            return DocumentUploadResponse.builder()
                    .documentId(documentId)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .status("failed")
                    .message("文档处理失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 批量上传文档
     *
     * @param files   文件列表
     * @param request 请求参数
     * @return 上传响应列表
     */
    @Transactional
    public List<DocumentUploadResponse> uploadAndEmbedDocuments(List<MultipartFile> files, DocumentUploadRequest request) {
        List<DocumentUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(uploadAndEmbedDocument(file, request));
        }
        return responses;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件大小（限制50MB）
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过50MB");
        }

        // 检查文件类型
        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("pdf", "txt", "doc", "docx", "md", "html", "xml", "json");
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }

    /**
     * 解析文档
     */
    private List<Document> parseDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        AssertUtil.isNotNull(filename, "文件名不能为空");
        String extension = getFileExtension(filename).toLowerCase();

        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            if ("pdf".equals(extension)) {
                // 使用PDF Reader
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                return pdfReader.get();
            } else {
                // 使用Tika Reader处理其他格式
                TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
                return tikaReader.get();
            }
        } catch (Exception e) {
            log.error("文档解析失败: {}", e.getMessage(), e);
            // 如果解析失败，尝试直接读取文本内容
            String content = new String(file.getBytes());
            return Collections.singletonList(new Document(content));
        }
    }

    /**
     * 为文档添加元数据
     */
    private void enrichDocumentsWithMetadata(List<Document> documents, MultipartFile file,
                                              DocumentUploadRequest request, String documentId) {
        String filename = file.getOriginalFilename();
        String fileType = file.getContentType();

        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            
            // 添加基础元数据
            metadata.put("document_id", documentId);
            metadata.put("file_name", filename);
            metadata.put("file_type", fileType);
            metadata.put("user_id", request.getUserId().toString());
            metadata.put("upload_time", System.currentTimeMillis());

            // 添加可选元数据
            if (request.getCategory() != null) {
                metadata.put("category", request.getCategory());
            }
            if (request.getDescription() != null) {
                metadata.put("description", request.getDescription());
            }

            // 添加自定义元数据
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }
        }
    }

    /**
     * 文档分块
     */
    private List<Document> chunkDocuments(List<Document> documents, DocumentUploadRequest request) {
        int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 800;
        int chunkOverlap = request.getChunkOverlap() != null ? request.getChunkOverlap() : 200;

        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        
        List<Document> chunkedDocuments = new ArrayList<>();
        for (Document doc : documents) {
            List<Document> chunks = splitter.apply(Collections.singletonList(doc));
            chunkedDocuments.addAll(chunks);
        }
        
        return chunkedDocuments;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
    }
}

