package com.luna.deepluna.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 文档元数据实体
 * 用于记录上传文档的基本信息（不存储原文件）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_metadata")
public class DocumentMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 文档唯一标识
     */
    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;
    
    /**
     * 文件名
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    /**
     * 文件类型
     */
    @Column(name = "file_type")
    private String fileType;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 文档分类/标签
     */
    @Column(name = "category")
    private String category;
    
    /**
     * 文档描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 向量数量
     */
    @Column(name = "vector_count")
    private Integer vectorCount;
    
    /**
     * 状态：success, failed, processing
     */
    @Column(name = "status")
    private String status;
    
    /**
     * 上传时间
     */
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
    
    /**
     * 是否已删除
     */
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
