/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ChatRequest } from '../models/ChatRequest';
import type { SseEmitter } from '../models/SseEmitter';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class ChatControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 流式聊天接口
     * 处理用户聊天消息，支持流式响应、问题澄清和研究任务执行
     * @param requestBody
     * @returns SseEmitter OK
     * @throws ApiError
     */
    public chatStream(
        requestBody: ChatRequest,
    ): CancelablePromise<SseEmitter> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/v1/chat/stream',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
}
