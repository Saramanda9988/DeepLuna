/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 文档上传响应
 */
export type DocumentUploadResponse = {
    /**
     * 文档ID
     */
    documentId?: string;
    /**
     * 文档名称
     */
    fileName?: string;
    /**
     * 文档类型
     */
    fileType?: string;
    /**
     * 文件大小（字节）
     */
    fileSize?: number;
    /**
     * 向量数量
     */
    vectorCount?: number;
    /**
     * 处理状态
     */
    status?: string;
    /**
     * 处理消息
     */
    message?: string;
};

