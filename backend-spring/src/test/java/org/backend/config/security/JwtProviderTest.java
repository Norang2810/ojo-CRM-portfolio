package org.backend.config.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "01234567890123456789012345678901",
                Duration.ofHours(1),
                Duration.ofDays(7)
        );
        jwtProvider.init();
    }

    @Test
    void issuesAccessAndRefreshWithDistinctJtiAndSharedSid() {
        String sid = UUID.randomUUID().toString();
        String fingerprint = "fingerprint-hash";

        JwtProvider.TokenPair pair = jwtProvider.generateTokenPair(
                1L,
                "admin@example.com",
                "ADMIN",
                sid,
                fingerprint
        );

        Claims access = jwtProvider.getClaims(pair.accessToken());
        Claims refresh = jwtProvider.getClaims(pair.refreshToken());

        assertThat(access.getId()).isEqualTo(pair.accessTokenId());
        assertThat(refresh.getId()).isEqualTo(pair.refreshTokenId());
        assertThat(access.getId()).isNotEqualTo(refresh.getId());
        assertThat(access.get("sid", String.class)).isEqualTo(sid);
        assertThat(refresh.get("sid", String.class)).isEqualTo(sid);
        assertThat(access.get("fp", String.class)).isEqualTo(fingerprint);
        assertThat(refresh.get("fp", String.class)).isEqualTo(fingerprint);
        assertThat(jwtProvider.isAccessToken(pair.accessToken())).isTrue();
        assertThat(jwtProvider.isRefreshToken(pair.refreshToken())).isTrue();
    }

    @Test
    void rotationAlwaysChangesBothTokenIdentifiers() {
        String sid = UUID.randomUUID().toString();

        JwtProvider.TokenPair first = jwtProvider.generateTokenPair(1L, "a@b.com", "ADMIN", sid, "fp");
        JwtProvider.TokenPair second = jwtProvider.generateTokenPair(1L, "a@b.com", "ADMIN", sid, "fp");

        assertThat(second.sessionId()).isEqualTo(first.sessionId());
        assertThat(second.accessTokenId()).isNotEqualTo(first.accessTokenId());
        assertThat(second.refreshTokenId()).isNotEqualTo(first.refreshTokenId());
        assertThat(second.accessToken()).isNotEqualTo(first.accessToken());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
    }
}
