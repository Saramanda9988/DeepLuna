package com.luna.deepluna.domain.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档上传请求")
public class DocumentUploadRequest {

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "文档分类/标签", example = "research,技术文档")
    private String category;

    @Schema(description = "文档描述")
    private String description;

    @Schema(description = "是否分块", example = "true")
    private Boolean enableChunking = true;

    @Schema(description = "分块大小", example = "800")
    private Integer chunkSize = 800;

    @Schema(description = "分块重叠大小", example = "200")
    private Integer chunkOverlap = 200;

    @Schema(description = "文档元数据")
    private Map<String, Object> metadata;
}
