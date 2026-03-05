package com.luna.deepluna.common.security;

import com.luna.deepluna.common.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private final AuthProperties authProperties;

    public String generateToken(Long userId, String userName) {
        Instant now = Instant.now();
        long ttl = authProperties.getJwt().getExpirationSeconds();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userName", userName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .signWith(getSigningKey())
                .compact();
    }

    public Optional<JwtUserPrincipal> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String userName = claims.get("userName", String.class);
        return Optional.of(new JwtUserPrincipal(Long.valueOf(userId), userName));
    }

    private SecretKey getSigningKey() {
        byte[] secret = authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(secret);
    }
}
