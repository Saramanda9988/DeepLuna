/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultListChatHistoryResponse } from '../models/ApiResultListChatHistoryResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class ChatHistoryControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 查询会话聊天历史
     * @param sessionId
     * @returns ApiResultListChatHistoryResponse OK
     * @throws ApiError
     */
    public getSessionChatHistory(
        sessionId: string,
    ): CancelablePromise<ApiResultListChatHistoryResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/chat-history/list/{sessionId}',
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
