package com.luna.deepluna;

import com.luna.deepluna.agent.agentTool.WebSearchProvider;
import com.luna.deepluna.agent.agentTool.WebSearchProviderRegistry;
import com.luna.deepluna.common.exception.BusinessException;
import com.luna.deepluna.domain.response.websearch.WebSearchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSearchProviderRegistryTest {

    @Test
    void getProvider_shouldResolveByProviderId() {
        WebSearchProvider tavilyProvider = provider("tavily");
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(tavilyProvider));

        WebSearchProvider resolved = registry.getProvider("TAVILY");

        assertSame(tavilyProvider, resolved);
    }

    @Test
    void getProvider_shouldThrowWhenMissing() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(provider("tavily")));

        assertThrows(BusinessException.class, () -> registry.getProvider("bing"));
    }

    private WebSearchProvider provider(String id) {
        return new WebSearchProvider() {
            @Override
            public String providerId() {
                return id;
            }

            @Override
            public WebSearchResponse search(String query) {
                return null;
            }
        };
    }
}

