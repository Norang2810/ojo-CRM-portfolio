package org.backend.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final String secret;
    private SecretKey key;

    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity:1h}") Duration accessTokenValidity,
            @Value("${app.jwt.refresh-token-validity:7d}") Duration refreshTokenValidity
    ) {
        this.secret = secret;
        this.accessTokenValidityMs = accessTokenValidity.toMillis();
        this.refreshTokenValidityMs = refreshTokenValidity.toMillis();
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // role: "CS" | "MARKETING" | "ADMIN"
    public TokenPair generateTokenPair(
            Long adminId,
            String email,
            String role,
            String sessionId,
            String fingerprint
    ) {
        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant refreshExpiresAt = now.plusMillis(refreshTokenValidityMs);

        String accessToken = generateAccessToken(
                adminId,
                email,
                role,
                sessionId,
                accessTokenId,
                fingerprint,
                now
        );
        String refreshToken = generateRefreshToken(
                adminId,
                sessionId,
                refreshTokenId,
                fingerprint,
                now,
                refreshExpiresAt
        );

        return new TokenPair(
                accessToken,
                refreshToken,
                sessionId,
                accessTokenId,
                refreshTokenId,
                refreshExpiresAt
        );
    }

    private String generateAccessToken(
            Long adminId,
            String email,
            String role,
            String sessionId,
            String tokenId,
            String fingerprint,
            Instant issuedAt
    ) {
        Date now = Date.from(issuedAt);
        Date exp = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .id(tokenId)
                .claim("email", email)
                .claim("role", role)      // "ADMIN" 같이 ROLE_ 없는 값
                .claim("typ", "access")
                .claim("sid", sessionId)
                .claim("fp", fingerprint)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    private String generateRefreshToken(
            Long adminId,
            String sessionId,
            String tokenId,
            String fingerprint,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Date now = Date.from(issuedAt);
        Date exp = Date.from(expiresAt);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .id(tokenId)
                .claim("typ", "refresh")
                .claim("sid", sessionId)
                .claim("fp", fingerprint)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            Object typ = getClaims(token).get("typ");
            return typ != null && "refresh".equals(String.valueOf(typ));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Object typ = getClaims(token).get("typ");
            return typ != null && "access".equals(String.valueOf(typ));
        } catch (Exception e) {
            return false;
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            String sessionId,
            String accessTokenId,
            String refreshTokenId,
            Instant refreshExpiresAt
    ) {
    }
}
