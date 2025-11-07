package com.luna.deepluna.service.factory;

import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.Model;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomModelFactory {
    @Autowired
    private OpenAiChatModel baseChatModel;

    @Autowired
    private OpenAiApi baseOpenAiApi;

    // 创建自定义模型的ChatModel客户端
    public OpenAiChatModel createChatModelClient(Model config) {
        AssertUtil.isNotNull(config, "模型配置不能为空");
        if (config.getName() == null || config.getUrl() == null || config.getToken() == null) {
            throw new BusinessException("模型配置缺少必要参数");
        }
        OpenAiApi customApi = baseOpenAiApi.mutate()
                .baseUrl(config.getUrl())
                .apiKey(config.getToken())
                .build();
        return baseChatModel.mutate()
                .openAiApi(customApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getName())
                        .temperature(0.5)
                        .build())
                .build();
    }
}
