export type SessionProgressSubAgentResponse = {
    subAgentId?: string;
    researchTopic?: string;
    status?: SessionProgressSubAgentResponse.status;
    updatedAt?: string;
};

export namespace SessionProgressSubAgentResponse {
    export enum status {
        PENDING = 'PENDING',
        IN_PROGRESS = 'IN_PROGRESS',
        COMPLETED = 'COMPLETED',
        FAILED = 'FAILED',
    }
}
