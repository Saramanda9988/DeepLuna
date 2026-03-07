/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultMapStringString } from '../models/ApiResultMapStringString';
import type { ApiResultVoid } from '../models/ApiResultVoid';
import type { WebSearchProviderConfigRequest } from '../models/WebSearchProviderConfigRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class SystemConfigControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 获取当前 WebSearch Provider
     * @returns ApiResultMapStringString OK
     * @throws ApiError
     */
    public getWebSearchProvider(): CancelablePromise<ApiResultMapStringString> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/config/websearch/provider',
            errors: {
                400: `Bad Request`,
                405: `Method Not Allowed`,
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * 更新当前 WebSearch Provider
     * @param requestBody
     * @returns ApiResultVoid OK
     * @throws ApiError
     */
    public updateWebSearchProvider(
        requestBody: WebSearchProviderConfigRequest,
    ): CancelablePromise<ApiResultVoid> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/capi/config/websearch/provider',
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
