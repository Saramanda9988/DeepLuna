package com.luna.deepluna.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * off: local single-user mode (no auth)
     * jwt: requires Bearer JWT for protected APIs
     */
    private String mode = "off";

    private Jwt jwt = new Jwt();

    public boolean isJwtMode() {
        return "jwt".equalsIgnoreCase(mode);
    }

    @Data
    public static class Jwt {
        private String secret = "local-dev-jwt-secret-change-me-please";
        private long expirationSeconds = 86400;
    }
}
