package com.luna.deepluna.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProdSecretGuard {

    private static final Set<String> BLOCKED_EXACT_VALUES = Set.of(
            "your-openai-api-key",
            "your-deepseek-api-key-here",
            "your-zhipuai-api-key-here",
            "your-tavily-api-key-here",
            "sk-e2033644f3b948e1b0083ff72ac13b2c",
            "fcf7c855a7e143539f2035b6511bc728.KJaVwVaTVB4cMa0B",
            "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",
            "local-dev-jwt-secret-change-me-please"
    );

    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "your-",
            "placeholder",
            "replace-me",
            "change-me"
    );

    private final Environment environment;
    private final AuthProperties authProperties;

    @org.springframework.beans.factory.annotation.Value("${spring.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @org.springframework.beans.factory.annotation.Value("${spring.ai.zhipuai.api-key:}")
    private String zhipuApiKey;

    @org.springframework.beans.factory.annotation.Value("${websearch.tavily.api.key:}")
    private String tavilyApiKey;

    @PostConstruct
    public void validateOnStartup() {
        if (!isProdProfileActive()) {
            return;
        }
        assertSecure("deepseek.api-key", deepseekApiKey);
        assertSecure("zhipuai.api-key", zhipuApiKey);
        assertSecure("websearch.tavily.api-key", tavilyApiKey);

        if (authProperties.isJwtMode()) {
            assertSecure("AUTH_JWT_SECRET", authProperties.getJwt().getSecret());
        }

        log.info("ProdSecretGuard passed");
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private void assertSecure(String keyName, String keyValue) {
        if (!StringUtils.hasText(keyValue)) {
            throw new IllegalStateException("prod profile requires non-empty key: " + keyName);
        }
        String normalized = keyValue.trim().toLowerCase();
        if (BLOCKED_EXACT_VALUES.contains(keyValue.trim()) ||
                BLOCKED_KEYWORDS.stream().anyMatch(normalized::contains)) {
            throw new IllegalStateException("prod profile detected placeholder/default key: " + keyName);
        }
    }
}
