package com.luna.deepluna.controller;

import com.luna.deepluna.agent.agentTool.WebSearchProviderRegistry;
import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.domain.request.WebSearchProviderConfigRequest;
import com.luna.deepluna.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/capi/config")
@RequiredArgsConstructor
@Tag(name = "SystemConfigController", description = "系统配置管理接口")
public class SystemConfigController {

    private final AppConfigService appConfigService;
    private final WebSearchProviderRegistry webSearchProviderRegistry;

    @GetMapping("/websearch/provider")
    @Operation(summary = "获取当前 WebSearch Provider")
    public ApiResult<Map<String, String>> getWebSearchProvider() {
        String providerId = appConfigService.getWebSearchProvider();
        return ApiResult.success(Map.of("providerId", providerId));
    }

    @PutMapping("/websearch/provider")
    @Operation(summary = "更新当前 WebSearch Provider")
    public ApiResult<Void> updateWebSearchProvider(@RequestBody WebSearchProviderConfigRequest request) {
        String providerId = request == null ? null : request.getProviderId();
        webSearchProviderRegistry.getProvider(providerId);
        appConfigService.setWebSearchProvider(providerId);
        return ApiResult.success();
    }
}

