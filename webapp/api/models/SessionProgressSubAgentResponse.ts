/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 会话研究进度中的子任务信息
 */
export type SessionProgressSubAgentResponse = {
    /**
     * 子智能体ID
     */
    subAgentId?: string;
    /**
     * 研究主题
     */
    researchTopic?: string;
    /**
     * 子智能体状态
     */
    status?: SessionProgressSubAgentResponse.status;
    /**
     * 最近更新时间
     */
    updatedAt?: string;
};
export namespace SessionProgressSubAgentResponse {
    /**
     * 子智能体状态
     */
    export enum status {
        PENDING = 'PENDING',
        IN_PROGRESS = 'IN_PROGRESS',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}

