/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultDocumentUploadResponse } from '../models/ApiResultDocumentUploadResponse';
import type { ApiResultListDocumentUploadResponse } from '../models/ApiResultListDocumentUploadResponse';
import type { ApiResultString } from '../models/ApiResultString';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class EmbeddingControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 上传单个文档并进行向量化
     * 支持PDF、TXT、DOC、DOCX、MD、HTML、XML、JSON等格式
     * @param userId 用户ID
     * @param category 文档分类/标签
     * @param description 文档描述
     * @param enableChunking 是否启用分块
     * @param chunkSize 分块大小
     * @param chunkOverlap 分块重叠大小
     * @param requestBody
     * @returns ApiResultDocumentUploadResponse OK
     * @throws ApiError
     */
    public uploadDocument(
        userId: number,
        category?: string,
        description?: string,
        enableChunking: boolean = true,
        chunkSize: number = 800,
        chunkOverlap: number = 200,
        requestBody?: {
            /**
             * 上传的文件
             */
            file: Blob;
        },
    ): CancelablePromise<ApiResultDocumentUploadResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/embedding/upload',
            query: {
                'userId': userId,
                'category': category,
                'description': description,
                'enableChunking': enableChunking,
                'chunkSize': chunkSize,
                'chunkOverlap': chunkOverlap,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 批量上传文档并进行向量化
     * 支持一次上传多个文档文件
     * @param userId 用户ID
     * @param category 文档分类/标签
     * @param description 文档描述
     * @param enableChunking 是否启用分块
     * @param chunkSize 分块大小
     * @param chunkOverlap 分块重叠大小
     * @param requestBody
     * @returns ApiResultListDocumentUploadResponse OK
     * @throws ApiError
     */
    public uploadDocuments(
        userId: number,
        category?: string,
        description?: string,
        enableChunking: boolean = true,
        chunkSize: number = 800,
        chunkOverlap: number = 200,
        requestBody?: {
            /**
             * 上传的文件列表
             */
            files: Array<Blob>;
        },
    ): CancelablePromise<ApiResultListDocumentUploadResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/embedding/upload/batch',
            query: {
                'userId': userId,
                'category': category,
                'description': description,
                'enableChunking': enableChunking,
                'chunkSize': chunkSize,
                'chunkOverlap': chunkOverlap,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 健康检查
     * 检查向量数据库连接状态
     * @returns ApiResultString OK
     * @throws ApiError
     */
    public healthCheck(): CancelablePromise<ApiResultString> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/embedding/health',
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
}
