package com.luna.deepluna.repository;

import com.luna.deepluna.domain.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档元数据仓库
 */
@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {
    
    /**
     * 根据文档ID查询
     */
    Optional<DocumentMetadata> findByDocumentId(String documentId);
    
    /**
     * 根据用户ID查询
     */
    List<DocumentMetadata> findByUserIdAndIsDeletedFalse(Long userId);
    
    /**
     * 根据分类查询
     */
    List<DocumentMetadata> findByCategoryAndIsDeletedFalse(String category);
    
    /**
     * 根据用户ID和分类查询
     */
    List<DocumentMetadata> findByUserIdAndCategoryAndIsDeletedFalse(Long userId, String category);
}
