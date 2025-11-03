package com.luna.deepluna.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档上传响应")
public class DocumentUploadResponse {

    @Schema(description = "文档ID")
    private String documentId;

    @Schema(description = "文档名称")
    private String fileName;

    @Schema(description = "文档类型")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "向量数量")
    private Integer vectorCount;

    @Schema(description = "处理状态", example = "success")
    private String status;

    @Schema(description = "处理消息")
    private String message;
}
