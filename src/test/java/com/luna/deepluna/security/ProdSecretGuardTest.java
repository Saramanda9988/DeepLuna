package com.luna.deepluna.security;

import com.luna.deepluna.common.config.AuthProperties;
import com.luna.deepluna.common.config.ProdSecretGuard;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class ProdSecretGuardTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("auth.jwt.secret=test-jwt-secret-test-jwt-secret-1234567890");

    @Test
    void shouldAllowPlaceholderKeysInLocalProfile() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=local",
                        "spring.ai.deepseek.api-key=your-deepseek-api-key-here",
                        "spring.ai.zhipuai.api-key=your-zhipuai-api-key-here",
                        "websearch.tavily.api.key=your-tavily-api-key-here"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldFailStartupInProdWhenPlaceholderKeyExists() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=prod",
                        "spring.ai.deepseek.api-key=your-deepseek-api-key-here",
                        "spring.ai.zhipuai.api-key=zhipu-real-key",
                        "websearch.tavily.api.key=tavily-real-key"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("placeholder/default key: DEEPSEEK_API_KEY");
                });
    }

    @Test
    void shouldFailStartupInProdWhenJwtSecretIsDefaultInJwtMode() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=prod",
                        "auth.mode=jwt",
                        "auth.jwt.secret=local-dev-jwt-secret-change-me-please",
                        "spring.ai.deepseek.api-key=deepseek-real-key",
                        "spring.ai.zhipuai.api-key=zhipu-real-key",
                        "websearch.tavily.api.key=tavily-real-key"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("placeholder/default key: AUTH_JWT_SECRET");
                });
    }

    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class TestConfig {
        @Bean
        ProdSecretGuard prodSecretGuard(Environment environment, AuthProperties authProperties) {
            return new ProdSecretGuard(environment, authProperties);
        }
    }
}
