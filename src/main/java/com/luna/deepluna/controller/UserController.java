package com.luna.deepluna.controller;

import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.domain.request.LoginRequest;
import com.luna.deepluna.domain.request.RegisterRequest;
import com.luna.deepluna.domain.response.UserResponse;
import com.luna.deepluna.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/capi/user")
@RequiredArgsConstructor
@Tag(name = "UserController", description = "用户管理接口")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResult<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse userResponse = userService.register(request);
        return ApiResult.success(userResponse);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResult<UserResponse> login(@RequestBody LoginRequest request) {
        UserResponse userResponse = userService.login(request);
        return ApiResult.success(userResponse);
    }
    
    /**
     * 用户退出登录
     */
    @PostMapping("/logout/{userId}")
    @Operation(summary = "用户退出登录")
    public ApiResult<Void> logout(@PathVariable Long userId) {
        userService.logout(userId);
        return ApiResult.success();
    }
    
    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{userId}")
    @Operation(summary = "检查用户是否在线")
    public ApiResult<Boolean> isOnline(@PathVariable Long userId) {
        boolean online = userService.isOnline(userId);
        return ApiResult.success(online);
    }
}
