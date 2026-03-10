package com.luna.deepluna.controller;

import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.domain.response.SessionProgressSnapshotResponse;
import com.luna.deepluna.service.SessionProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "SessionProgressController", description = "会话研究进度接口")
public class SessionProgressController {

    private final SessionProgressService sessionProgressService;

    @GetMapping("/capi/session/progress/{sessionId}")
    @Operation(summary = "获取会话研究进度快照")
    public ApiResult<SessionProgressSnapshotResponse> getSessionProgress(@PathVariable String sessionId) {
        return ApiResult.success(sessionProgressService.getSnapshot(sessionId));
    }

    @GetMapping(value = "/v1/session/progress/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅会话研究进度")
    public SseEmitter streamSessionProgress(@PathVariable String sessionId) {
        return sessionProgressService.subscribe(sessionId);
    }
}
