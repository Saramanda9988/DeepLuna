package com.luna.deepluna.service;

import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.request.ModelRequest;
import com.luna.deepluna.domain.response.ModelResponse;
import com.luna.deepluna.domain.entity.Model;
import com.luna.deepluna.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {
    
    private final ModelRepository modelRepository;

    private Model requireModel(String modelId) {
        Optional<Model> optionalModel = modelRepository.findById(modelId);
        return optionalModel.orElseThrow(() -> new BusinessException("模型不存在"));
    }

    private ModelResponse toResponse(Model model) {
        return new ModelResponse(
                model.getModelId(),
                model.getName(),
                model.getUrl(),
                model.getCreateTime()
        );
    }
    
    /**
     * 查询所有模型列表（不包含token）
     */
    public List<ModelResponse> getAllModels() {
        List<Model> models = modelRepository.findAll();
        
        return models.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查询模型（不包含token）
     */
    public ModelResponse getModelById(String modelId) {
        return toResponse(requireModel(modelId));
    }

    /**
     * 根据ID查询模型（包含token，仅服务内部使用）
     */
    public Model getModelEntityById(String modelId) {
        return requireModel(modelId);
    }
    
    /**
     * 创建新模型
     */
    public ModelResponse createModel(ModelRequest request) {
        String modelId = UUID.randomUUID().toString();
        
        Model model = Model.builder()
                .modelId(modelId)
                .name(request.getName())
                .token(request.getToken())
                .url(request.getUrl())
                .createTime(Instant.now())
                .build();
        
        modelRepository.save(model);
        
        log.info("创建模型成功: modelId={}, name={}", modelId, request.getName());

        return toResponse(model);
    }
    
    /**
     * 更新模型信息
     */
    public ModelResponse updateModel(String modelId, ModelRequest request) {
        Model model = requireModel(modelId);
        
        if (request.getName() != null) {
            model.setName(request.getName());
        }
        
        if (request.getToken() != null) {
            model.setToken(request.getToken());
        }
        
        if (request.getUrl() != null) {
            model.setUrl(request.getUrl());
        }
        
        modelRepository.save(model);
        
        log.info("更新模型成功: modelId={}, name={}", modelId, model.getName());

        return toResponse(model);
    }
    
    /**
     * 删除模型
     */
    public void deleteModel(String modelId) {
        requireModel(modelId);
        
        modelRepository.deleteById(modelId);
        
        log.info("删除模型成功: modelId={}", modelId);
    }
}
