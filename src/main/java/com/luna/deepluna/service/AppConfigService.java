package com.luna.deepluna.service;

import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.entity.AppConfig;
import com.luna.deepluna.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    public static final String WEBSEARCH_PROVIDER_KEY = "websearch.provider";

    private final AppConfigRepository appConfigRepository;
    private final ConcurrentMap<String, String> configCache = new ConcurrentHashMap<>();

    @Value("${websearch.provider:tavily}")
    private String webSearchProviderDefault;

    public String getWebSearchProvider() {
        return getConfig(WEBSEARCH_PROVIDER_KEY, webSearchProviderDefault);
    }

    @Transactional
    public void setWebSearchProvider(String providerId) {
        AssertUtil.isNotEmpty(providerId, "websearch.provider不能为空");
        String normalizedProviderId = providerId.trim().toLowerCase();

        AppConfig appConfig = appConfigRepository.findById(WEBSEARCH_PROVIDER_KEY)
                .orElseGet(() -> AppConfig.builder().configKey(WEBSEARCH_PROVIDER_KEY).build());
        appConfig.setConfigValue(normalizedProviderId);
        appConfig.setDescription("Active web search provider id");

        appConfigRepository.save(appConfig);
        configCache.put(WEBSEARCH_PROVIDER_KEY, normalizedProviderId);
    }

    public String getConfig(String key, String defaultValue) {
        String cachedValue = configCache.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        Optional<AppConfig> appConfig = appConfigRepository.findById(key);
        String value = appConfig
                .map(AppConfig::getConfigValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);

        configCache.put(key, value);
        return value;
    }
}

