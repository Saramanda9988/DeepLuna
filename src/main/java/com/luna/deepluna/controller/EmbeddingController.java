package com.luna.deepluna.controller;

import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.domain.request.DocumentUploadRequest;
import com.luna.deepluna.domain.response.DocumentUploadResponse;
import com.luna.deepluna.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
@Tag(name = "EmbeddingController", description = "上传文档进行embed控制器")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping("/upload")
    @Operation(summary = "上传单个文档并进行向量化", description = "支持PDF、TXT、DOC、DOCX、MD、HTML、XML、JSON等格式")
    public ApiResult<DocumentUploadResponse> uploadDocument(
            @Parameter(description = "上传的文件", required = true)
            @RequestPart("file") MultipartFile file,
            
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,
            
            @Parameter(description = "文档分类/标签", example = "research,技术文档")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "文档描述")
            @RequestParam(required = false) String description,
            
            @Parameter(description = "是否启用分块", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean enableChunking,
            
            @Parameter(description = "分块大小", example = "800")
            @RequestParam(required = false, defaultValue = "800") Integer chunkSize,
            
            @Parameter(description = "分块重叠大小", example = "200")
            @RequestParam(required = false, defaultValue = "200") Integer chunkOverlap) {
        
        log.info("接收到文档上传请求: 文件名={}, 用户ID={}", file.getOriginalFilename(), userId);
        
        // 构建请求对象
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .userId(userId)
                .category(category)
                .description(description)
                .enableChunking(enableChunking)
                .chunkSize(chunkSize)
                .chunkOverlap(chunkOverlap)
                .build();
        
        // 处理文档
        DocumentUploadResponse response = embeddingService.uploadAndEmbedDocument(file, request);

        return ApiResult.success(response);
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文档并进行向量化", description = "支持一次上传多个文档文件")
    public ApiResult<List<DocumentUploadResponse>> uploadDocuments(
            @Parameter(description = "上传的文件列表", required = true)
            @RequestPart("files") List<MultipartFile> files,
            
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,
            
            @Parameter(description = "文档分类/标签", example = "research,技术文档")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "文档描述")
            @RequestParam(required = false) String description,
            
            @Parameter(description = "是否启用分块", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean enableChunking,
            
            @Parameter(description = "分块大小", example = "800")
            @RequestParam(required = false, defaultValue = "800") Integer chunkSize,
            
            @Parameter(description = "分块重叠大小", example = "200")
            @RequestParam(required = false, defaultValue = "200") Integer chunkOverlap) {
        
        log.info("接收到批量文档上传请求: 文件数量={}, 用户ID={}", files.size(), userId);
        
        // 构建请求对象
        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setUserId(userId);
        request.setCategory(category);
        request.setDescription(description);
        request.setEnableChunking(enableChunking);
        request.setChunkSize(chunkSize);
        request.setChunkOverlap(chunkOverlap);
        
        // 批量处理文档
        List<DocumentUploadResponse> responses = embeddingService.uploadAndEmbedDocuments(files, request);
        
        return ApiResult.success(responses);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查向量数据库连接状态")
    public ApiResult<String> healthCheck() {
        return ApiResult.success("Embedding service is running");
    }
}

