package org.backend.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final String secret;
    private SecretKey key;

    private final long accessTokenValidityMs = 60 * 60 * 1000L; //at : 1일
    private final long refreshTokenValidityMs = 7L * 24 * 60 * 60 * 1000L; //rt:7일

    public JwtProvider(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // role: "CS" | "MARKETING" | "ADMIN"
    public String generateAccessToken(Long adminId, String email, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("email", email)
                .claim("role", role)      // "ADMIN" 같이 ROLE_ 없는 값
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long adminId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("typ", "refresh")
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
}