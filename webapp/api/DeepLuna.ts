/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseHttpRequest } from './core/BaseHttpRequest';
import type { OpenAPIConfig } from './core/OpenAPI';
import { FetchHttpRequest } from './core/FetchHttpRequest';
import { ChatControllerService } from './services/ChatControllerService';
import { EmbeddingControllerService } from './services/EmbeddingControllerService';
import { ModelControllerService } from './services/ModelControllerService';
import { SessionControllerService } from './services/SessionControllerService';
import { SystemConfigControllerService } from './services/SystemConfigControllerService';
import { UserControllerService } from './services/UserControllerService';
type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;
export class DeepLuna {
    public readonly chatController: ChatControllerService;
    public readonly embeddingController: EmbeddingControllerService;
    public readonly modelController: ModelControllerService;
    public readonly sessionController: SessionControllerService;
    public readonly systemConfigController: SystemConfigControllerService;
    public readonly userController: UserControllerService;
    public readonly request: BaseHttpRequest;
    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = FetchHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? 'http://localhost:8090',
            VERSION: config?.VERSION ?? '1.0',
            WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
            CREDENTIALS: config?.CREDENTIALS ?? 'include',
            TOKEN: config?.TOKEN,
            USERNAME: config?.USERNAME,
            PASSWORD: config?.PASSWORD,
            HEADERS: config?.HEADERS,
            ENCODE_PATH: config?.ENCODE_PATH,
        });
        this.chatController = new ChatControllerService(this.request);
        this.embeddingController = new EmbeddingControllerService(this.request);
        this.modelController = new ModelControllerService(this.request);
        this.sessionController = new SessionControllerService(this.request);
        this.systemConfigController = new SystemConfigControllerService(this.request);
        this.userController = new UserControllerService(this.request);
    }
}

