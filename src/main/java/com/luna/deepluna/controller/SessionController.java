package com.luna.deepluna.controller;

import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.dto.request.CreateSessionRequest;
import com.luna.deepluna.dto.request.UpdateSessionRequest;
import com.luna.deepluna.dto.response.SessionDetailResponse;
import com.luna.deepluna.dto.response.SessionResponse;
import com.luna.deepluna.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/capi/session")
@RequiredArgsConstructor
@Tag(name = "SessionController", description = "会话管理接口")
public class SessionController {
    
    private final SessionService sessionService;
    
    /**
     * 创建Session
     */
    @PostMapping("/create")
    @Operation(summary = "创建Session")
    public ApiResult<String> createSession(@RequestBody CreateSessionRequest request) {
        String sessionId = sessionService.createSession(request);
        return ApiResult.success(sessionId);
    }
    
    /**
     * 查询用户历史Session列表
     */
    @GetMapping("/list/{userId}")
    @Operation(summary = "查询用户历史Session列表")
    public ApiResult<List<SessionResponse>> getUserSessions(@PathVariable Long userId) {
        List<SessionResponse> sessions = sessionService.getUserSessions(userId);
        return ApiResult.success(sessions);
    }
    
    /**
     * 更新Session内容
     */
    @PutMapping("/update/{sessionId}")
    @Operation(summary = "更新Session内容")
    public ApiResult<Void> updateSession(@PathVariable String sessionId, 
                                          @RequestBody UpdateSessionRequest request) {
        sessionService.updateSession(sessionId, request);
        return ApiResult.success();
    }
    
    /**
     * 删除Session
     */
    @DeleteMapping("/delete/{sessionId}")
    @Operation(summary = "删除Session")
    public ApiResult<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return ApiResult.success();
    }
    
    /**
     * 获取Session详情
     */
    @GetMapping("/detail/{sessionId}")
    @Operation(summary = "获取Session详情")
    public ApiResult<SessionDetailResponse> getSessionDetail(@PathVariable String sessionId) {
        SessionDetailResponse detail = sessionService.getSessionDetail(sessionId);
        return ApiResult.success(detail);
    }
}
