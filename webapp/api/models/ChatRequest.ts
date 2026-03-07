/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 聊天请求
 */
export type ChatRequest = {
    /**
     * 聊天内容
     */
    message: string;
    /**
     * 使用的模型的id，如果不存在则使用默认
     */
    modelId?: string;
    /**
     * 会话ID
     */
    sessionId: string;
};

