import type { SessionProgressSubAgentResponse } from './SessionProgressSubAgentResponse';

export type SessionProgressSnapshotResponse = {
    sessionId?: string;
    sessionStatus?: SessionProgressSnapshotResponse.sessionStatus;
    supervisorState?: SessionProgressSnapshotResponse.supervisorState;
    latestMessage?: string;
    updatedAt?: string;
    totalSubAgents?: number;
    runningSubAgents?: number;
    completedSubAgents?: number;
    finished?: boolean;
    subAgents?: Array<SessionProgressSubAgentResponse>;
};

export namespace SessionProgressSnapshotResponse {
    export enum sessionStatus {
        IDLE = 'IDLE',
        CLARIFYING = 'CLARIFYING',
        RUNNING = 'RUNNING',
        REPORTING = 'REPORTING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }

    export enum supervisorState {
        IDLE = 'IDLE',
        INITIALIZING = 'INITIALIZING',
        RUNNING = 'RUNNING',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}
