package com.luna.deepluna.agent.agentTool;

import com.luna.deepluna.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WebSearchProviderRegistry {

    private final Map<String, WebSearchProvider> providers;

    public WebSearchProviderRegistry(List<WebSearchProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        provider -> provider.providerId().toLowerCase(),
                        Function.identity()
                ));
        log.info("Loaded web search providers: {}", this.providers.keySet());
    }

    public WebSearchProvider getProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new BusinessException("websearch.provider不能为空");
        }
        WebSearchProvider provider = providers.get(providerId.toLowerCase());
        if (provider == null) {
            throw new BusinessException("不支持的WebSearchProvider: " + providerId + ", 可选: " + providers.keySet());
        }
        return provider;
    }
}

