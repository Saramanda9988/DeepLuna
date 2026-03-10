/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultSessionProgressSnapshotResponse } from '../models/ApiResultSessionProgressSnapshotResponse';
import type { SseEmitter } from '../models/SseEmitter';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class SessionProgressControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 订阅会话研究进度
     * @param sessionId
     * @returns SseEmitter OK
     * @throws ApiError
     */
    public streamSessionProgress(
        sessionId: string,
    ): CancelablePromise<SseEmitter> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/v1/session/progress/{sessionId}/stream',
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
     * 获取会话研究进度快照
     * @param sessionId
     * @returns ApiResultSessionProgressSnapshotResponse OK
     * @throws ApiError
     */
    public getSessionProgress(
        sessionId: string,
    ): CancelablePromise<ApiResultSessionProgressSnapshotResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/session/progress/{sessionId}',
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
