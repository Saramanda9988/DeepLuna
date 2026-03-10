/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { SessionProgressSubAgentResponse } from './SessionProgressSubAgentResponse';
/**
 * 会话研究进度快照
 */
export type SessionProgressSnapshotResponse = {
    /**
     * 会话ID
     */
    sessionId?: string;
    /**
     * 会话状态
     */
    sessionStatus?: SessionProgressSnapshotResponse.sessionStatus;
    /**
     * Supervisor状态
     */
    supervisorState?: SessionProgressSnapshotResponse.supervisorState;
    /**
     * 最近一条进度消息
     */
    latestMessage?: string;
    /**
     * 最近更新时间
     */
    updatedAt?: string;
    /**
     * 子智能体总数
     */
    totalSubAgents?: number;
    /**
     * 运行中的子智能体数量
     */
    runningSubAgents?: number;
    /**
     * 已完成的子智能体数量
     */
    completedSubAgents?: number;
    /**
     * 是否已结束
     */
    finished?: boolean;
    /**
     * 子智能体列表
     */
    subAgents?: Array<SessionProgressSubAgentResponse>;
};
export namespace SessionProgressSnapshotResponse {
    /**
     * 会话状态
     */
    export enum sessionStatus {
        IDLE = 'IDLE',
        CLARIFYING = 'CLARIFYING',
        RUNNING = 'RUNNING',
        REPORTING = 'REPORTING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
    /**
     * Supervisor状态
     */
    export enum supervisorState {
        IDLE = 'IDLE',
        INITIALIZING = 'INITIALIZING',
        RUNNING = 'RUNNING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}

