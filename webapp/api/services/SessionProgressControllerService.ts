import type { ApiResultSessionProgressSnapshotResponse } from '../models/ApiResultSessionProgressSnapshotResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class SessionProgressControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}

    public getSessionProgress(
        sessionId: string,
    ): CancelablePromise<ApiResultSessionProgressSnapshotResponse> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/capi/session/progress/{sessionId}',
            path: {
                sessionId: sessionId,
            },
            errors: {
                400: `Bad Request`,
                404: `Not Found`,
                500: `Internal Server Error`,
            },
        });
    }

    public buildSessionProgressStreamUrl(sessionId: string): string {
        const base = this.httpRequest.config.BASE.replace(/\/$/, '');
        return `${base}/v1/session/progress/${encodeURIComponent(sessionId)}/stream`;
    }
}
