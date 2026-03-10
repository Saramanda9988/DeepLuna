import type { SessionProgressSnapshotResponse } from './SessionProgressSnapshotResponse';

export type ApiResultSessionProgressSnapshotResponse = {
    success: boolean;
    errCode?: number;
    errMsg?: string;
    data?: SessionProgressSnapshotResponse;
};
