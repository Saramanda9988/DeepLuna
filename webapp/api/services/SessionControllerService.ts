/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultListSessionResponse } from '../models/ApiResultListSessionResponse';
import type { ApiResultSessionDetailResponse } from '../models/ApiResultSessionDetailResponse';
import type { ApiResultString } from '../models/ApiResultString';
import type { ApiResultVoid } from '../models/ApiResultVoid';
import type { CreateSessionRequest } from '../models/CreateSessionRequest';
import type { UpdateSessionRequest } from '../models/UpdateSessionRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class SessionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 更新Session内容
     * @param sessionId
     * @param requestBody
     * @returns ApiResultVoid OK
     * @throws ApiError
     */
    public updateSession(
        sessionId: string,
        requestBody: UpdateSessionRequest,
    ): CancelablePromise<ApiResultVoid> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/capi/session/update/{sessionId}',
            path: {
                'sessionId': sessionId,
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
     * 创建Session
     * @param requestBody
     * @returns ApiResultString OK
     * @throws ApiError
     */
    public createSession(
        requestBody: CreateSessionRequest,
    ): CancelablePromise<ApiResultString> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/capi/session/create',
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
     * 查询用户历史Session列表
     * @param userId
     * @returns ApiResultListSessionResponse OK
     * @throws ApiError
     */
    public getUserSessions(
        userId: number,
    ): CancelablePromise<ApiResultListSessionResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/session/list/{userId}',
            path: {
                'userId': userId,
            },
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 获取Session详情
     * @param sessionId
     * @returns ApiResultSessionDetailResponse OK
     * @throws ApiError
     */
    public getSessionDetail(
        sessionId: string,
    ): CancelablePromise<ApiResultSessionDetailResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/session/detail/{sessionId}',
            path: {
                'sessionId': sessionId,
            },
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 删除Session
     * @param sessionId
     * @returns ApiResultVoid OK
     * @throws ApiError
     */
    public deleteSession(
        sessionId: string,
    ): CancelablePromise<ApiResultVoid> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/capi/session/delete/{sessionId}',
            path: {
                'sessionId': sessionId,
            },
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
}
