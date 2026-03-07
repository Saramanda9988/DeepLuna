/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultListModelResponse } from '../models/ApiResultListModelResponse';
import type { ApiResultModelResponse } from '../models/ApiResultModelResponse';
import type { ApiResultVoid } from '../models/ApiResultVoid';
import type { ModelRequest } from '../models/ModelRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class ModelControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 更新模型信息
     * @param modelId
     * @param requestBody
     * @returns ApiResultModelResponse OK
     * @throws ApiError
     */
    public updateModel(
        modelId: string,
        requestBody: ModelRequest,
    ): CancelablePromise<ApiResultModelResponse> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/capi/model/update/{modelId}',
            path: {
                'modelId': modelId,
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
     * 创建新模型
     * @param requestBody
     * @returns ApiResultModelResponse OK
     * @throws ApiError
     */
    public createModel(
        requestBody: ModelRequest,
    ): CancelablePromise<ApiResultModelResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/capi/model/create',
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
     * 根据ID查询模型
     * @param modelId
     * @returns ApiResultModelResponse OK
     * @throws ApiError
     */
    public getModelById(
        modelId: string,
    ): CancelablePromise<ApiResultModelResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/model/{modelId}',
            path: {
                'modelId': modelId,
            },
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 查询所有模型列表
     * @returns ApiResultListModelResponse OK
     * @throws ApiError
     */
    public getAllModels(): CancelablePromise<ApiResultListModelResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/model/list',
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 删除模型
     * @param modelId
     * @returns ApiResultVoid OK
     * @throws ApiError
     */
    public deleteModel(
        modelId: string,
    ): CancelablePromise<ApiResultVoid> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/capi/model/delete/{modelId}',
            path: {
                'modelId': modelId,
            },
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
}
