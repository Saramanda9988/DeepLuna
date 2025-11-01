package com.luna.deepluna.controller;

import com.luna.deepluna.common.domain.ApiResult;
import com.luna.deepluna.domain.request.ModelRequest;
import com.luna.deepluna.domain.response.ModelResponse;
import com.luna.deepluna.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/capi/model")
@RequiredArgsConstructor
@Tag(name = "ModelController", description = "模型管理接口")
public class ModelController {
    
    private final ModelService modelService;
    
    /**
     * 查询所有模型列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有模型列表")
    public ApiResult<List<ModelResponse>> getAllModels() {
        List<ModelResponse> models = modelService.getAllModels();
        return ApiResult.success(models);
    }
    
    /**
     * 根据ID查询模型
     */
    @GetMapping("/{modelId}")
    @Operation(summary = "根据ID查询模型")
    public ApiResult<ModelResponse> getModelById(@PathVariable String modelId) {
        ModelResponse model = modelService.getModelById(modelId);
        return ApiResult.success(model);
    }
    
    /**
     * 创建新模型
     */
    @PostMapping("/create")
    @Operation(summary = "创建新模型")
    public ApiResult<ModelResponse> createModel(@RequestBody ModelRequest request) {
        ModelResponse model = modelService.createModel(request);
        return ApiResult.success(model);
    }
    
    /**
     * 更新模型信息
     */
    @PutMapping("/update/{modelId}")
    @Operation(summary = "更新模型信息")
    public ApiResult<ModelResponse> updateModel(@PathVariable String modelId,
                                                @RequestBody ModelRequest request) {
        ModelResponse model = modelService.updateModel(modelId, request);
        return ApiResult.success(model);
    }
    
    /**
     * 删除模型
     */
    @DeleteMapping("/delete/{modelId}")
    @Operation(summary = "删除模型")
    public ApiResult<Void> deleteModel(@PathVariable String modelId) {
        modelService.deleteModel(modelId);
        return ApiResult.success();
    }
}
