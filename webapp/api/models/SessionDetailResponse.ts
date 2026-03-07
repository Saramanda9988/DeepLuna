/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 返回对象
 */
export type SessionDetailResponse = {
    sessionId?: string;
    userId?: number;
    model?: string;
    summary?: string;
    status?: SessionDetailResponse.status;
    researchBrief?: string;
    createdTime?: string;
    updateTime?: string;
};
export namespace SessionDetailResponse {
    export enum status {
        IDLE = 'IDLE',
        CLARIFYING = 'CLARIFYING',
        RUNNING = 'RUNNING',
        REPORTING = 'REPORTING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}

