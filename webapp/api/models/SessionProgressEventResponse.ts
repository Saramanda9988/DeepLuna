import type { SessionProgressSnapshotResponse } from './SessionProgressSnapshotResponse';
import type { SessionProgressSubAgentResponse } from './SessionProgressSubAgentResponse';

export type SessionProgressEventResponse = {
    sessionId?: string;
    eventType?: SessionProgressEventResponse.eventType;
    message?: string;
    sessionStatus?: SessionProgressEventResponse.sessionStatus;
    supervisorState?: SessionProgressEventResponse.supervisorState;
    subAgent?: SessionProgressSubAgentResponse;
    finished?: boolean;
    timestamp?: string;
    snapshot?: SessionProgressSnapshotResponse;
};

export namespace SessionProgressEventResponse {
    export enum eventType {
        SNAPSHOT = 'SNAPSHOT',
        SESSION_STATUS_CHANGED = 'SESSION_STATUS_CHANGED',
        SUPERVISOR_STATUS_CHANGED = 'SUPERVISOR_STATUS_CHANGED',
        SUB_AGENT_CREATED = 'SUB_AGENT_CREATED',
        SUB_AGENT_STATUS_CHANGED = 'SUB_AGENT_STATUS_CHANGED',
        RESEARCH_BRIEF_GENERATED = 'RESEARCH_BRIEF_GENERATED',
        MESSAGE = 'MESSAGE',
    }

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
