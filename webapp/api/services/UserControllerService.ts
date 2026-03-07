/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResultBoolean } from '../models/ApiResultBoolean';
import type { ApiResultUserResponse } from '../models/ApiResultUserResponse';
import type { ApiResultVoid } from '../models/ApiResultVoid';
import type { LoginRequest } from '../models/LoginRequest';
import type { RegisterRequest } from '../models/RegisterRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class UserControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * 用户注册
     * @param requestBody
     * @returns ApiResultUserResponse OK
     * @throws ApiError
     */
    public register(
        requestBody: RegisterRequest,
    ): CancelablePromise<ApiResultUserResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/capi/user/register',
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
     * 用户退出登录
     * @param userId
     * @returns ApiResultVoid OK
     * @throws ApiError
     */
    public logout(
        userId: number,
    ): CancelablePromise<ApiResultVoid> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/capi/user/logout/{userId}',
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
     * 用户登录
     * @param requestBody
     * @returns ApiResultUserResponse OK
     * @throws ApiError
     */
    public login(
        requestBody: LoginRequest,
    ): CancelablePromise<ApiResultUserResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/capi/user/login',
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
     * 检查用户是否在线
     * @param userId
     * @returns ApiResultBoolean OK
     * @throws ApiError
     */
    public isOnline(
        userId: number,
    ): CancelablePromise<ApiResultBoolean> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/user/online/{userId}',
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
}
