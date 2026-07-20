package org.backend.config.security.filter;

import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.backend.domain.auth.security.AdminPrincipal;
import org.backend.domain.auth.service.RevokedSessionStore;
import org.backend.domain.auth.service.TokenFingerprintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private AdminRepository adminRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private RevokedSessionStore revokedSessionStore;
    private TokenFingerprintService fingerprintService;
    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        adminRepository = mock(AdminRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        revokedSessionStore = mock(RevokedSessionStore.class);
        fingerprintService = new TokenFingerprintService("fingerprint-secret-012345678901234");
        jwtProvider = new JwtProvider(
                "01234567890123456789012345678901",
                Duration.ofHours(1),
                Duration.ofDays(7)
        );
        jwtProvider.init();
        filter = new JwtAuthenticationFilter(
                jwtProvider,
                adminRepository,
                refreshTokenRepository,
                revokedSessionStore,
                fingerprintService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesOnlyWhenSidAndFingerprintAreActive() throws Exception {
        String userAgent = "Mozilla/5.0 test-browser";
        String fingerprint = fingerprint(userAgent);
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair pair = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, fingerprint);
        Admin admin = mock(Admin.class);
        when(admin.getStatus()).thenReturn(AdminStatus.ACTIVE);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(refreshTokenRepository.existsBySessionIdAndAdminId(sid, 1L)).thenReturn(true);
        when(revokedSessionStore.isRevoked(sid)).thenReturn(false);

        MockHttpServletRequest request = request(pair.accessToken(), userAgent);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        AdminPrincipal principal = (AdminPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getSessionId()).isEqualTo(sid);
        assertThat(principal.getTokenId()).isEqualTo(pair.accessTokenId());
    }

    @Test
    void rejectsAccessTokenFromDifferentUserAgent() throws Exception {
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair pair = jwtProvider.generateTokenPair(
                1L,
                "admin@example.com",
                "ADMIN",
                sid,
                fingerprint("issued-browser")
        );

        filter.doFilter(
                request(pair.accessToken(), "different-browser"),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(adminRepository, refreshTokenRepository, revokedSessionStore);
    }

    @Test
    void rejectsAccessTokenWhenSidIsRevokedInRedis() throws Exception {
        String userAgent = "Mozilla/5.0";
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair pair = jwtProvider.generateTokenPair(
                1L,
                "admin@example.com",
                "ADMIN",
                sid,
                fingerprint(userAgent)
        );
        when(revokedSessionStore.isRevoked(sid)).thenReturn(true);

        filter.doFilter(request(pair.accessToken(), userAgent), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(adminRepository, refreshTokenRepository);
    }

    private MockHttpServletRequest request(String token, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader("User-Agent", userAgent);
        return request;
    }

    private String fingerprint(String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", userAgent);
        return fingerprintService.from(request);
    }
}
