package com.luna.deepluna.service.factory;

import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.Model;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;

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
        String normalizedBaseUrl = normalizeBaseUrl(config.getUrl());
        OpenAiApi customApi = baseOpenAiApi.mutate()
                .baseUrl(normalizedBaseUrl)
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

    private String normalizeBaseUrl(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        AssertUtil.isTrue(!url.isEmpty(), "模型URL不能为空");

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/v1/chat/completions")) {
            url = url.substring(0, url.length() - "/v1/chat/completions".length());
        } else if (lower.endsWith("/chat/completions")) {
            url = url.substring(0, url.length() - "/chat/completions".length());
        } else if (lower.endsWith("/v1")) {
            url = url.substring(0, url.length() - "/v1".length());
        }

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        AssertUtil.isTrue(url.startsWith("http://") || url.startsWith("https://"), "模型URL必须以 http:// 或 https:// 开头");
        return url;
    }
}
