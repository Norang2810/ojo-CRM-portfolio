package org.backend.domain.auth.service;

import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminRole;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.entity.RefreshToken;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenSessionServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private RevokedSessionStore revokedSessionStore;

    private JwtProvider jwtProvider;
    private TokenHashService tokenHashService;
    private TokenSessionService tokenSessionService;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "01234567890123456789012345678901",
                Duration.ofHours(1),
                Duration.ofDays(7)
        );
        jwtProvider.init();
        tokenHashService = new TokenHashService();
        TokenFingerprintService fingerprintService = new TokenFingerprintService(
                "fingerprint-secret-012345678901234"
        );
        tokenSessionService = new TokenSessionService(
                refreshTokenRepository,
                adminRepository,
                jwtProvider,
                tokenHashService,
                fingerprintService,
                revokedSessionStore
        );
    }

    @Test
    void storesOnlyRefreshTokenHashWhenCreatingSession() {
        Admin admin = activeAdmin();

        JwtProvider.TokenPair pair = tokenSessionService.createSession(admin, "fp");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getSessionId()).isEqualTo(pair.sessionId());
        assertThat(saved.getRefreshJti()).isEqualTo(pair.refreshTokenId());
        assertThat(saved.getTokenHash()).isEqualTo(tokenHashService.hash(pair.refreshToken()));
        assertThat(saved.getTokenHash()).isNotEqualTo(pair.refreshToken());
        assertThat(saved.getFingerprintHash()).isEqualTo("fp");
    }

    @Test
    void rotatesRefreshJtiUnderSameSession() {
        Admin admin = activeAdmin();
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        RefreshToken session = sessionOf(current, "fp");
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.of(session));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        JwtProvider.TokenPair rotated = tokenSessionService.rotate(current.refreshToken(), "fp");

        assertThat(rotated.sessionId()).isEqualTo(sid);
        assertThat(rotated.refreshTokenId()).isNotEqualTo(current.refreshTokenId());
        assertThat(session.getRefreshJti()).isEqualTo(rotated.refreshTokenId());
        assertThat(session.getTokenHash()).isEqualTo(tokenHashService.hash(rotated.refreshToken()));
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void detectsReuseOfRotatedRefreshTokenAndRevokesTokenFamily() {
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair reused = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        RefreshToken session = sessionOf(current, "fp");
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> tokenSessionService.rotate(reused.refreshToken(), "fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_MISMATCH));

        verify(refreshTokenRepository).delete(session);
        verify(revokedSessionStore).revokeAfterCommit(sid, "refresh-reuse");
    }

    @Test
    void rejectsDifferentFingerprintAndRevokesSession() {
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "issued-fp");
        RefreshToken session = sessionOf(current, "issued-fp");
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> tokenSessionService.rotate(current.refreshToken(), "different-fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOKEN_ENVIRONMENT_MISMATCH));

        verify(refreshTokenRepository).delete(session);
        verify(revokedSessionStore).revokeAfterCommit(sid, "fingerprint-mismatch");
    }

    @Test
    void keepsExpiredOrLoggedOutExceptionWhenSessionDoesNotExist() {
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenSessionService.rotate(current.refreshToken(), "fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED));
    }

    @Test
    void deletesServerSessionWhenItsStoredExpiryHasPassed() {
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        RefreshToken expired = RefreshToken.builder()
                .adminId(1L)
                .sessionId(sid)
                .refreshJti(current.refreshTokenId())
                .tokenHash(tokenHashService.hash(current.refreshToken()))
                .fingerprintHash("fp")
                .expiresAt(java.time.Instant.now().minusSeconds(1))
                .build();
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenSessionService.rotate(current.refreshToken(), "fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED));

        verify(refreshTokenRepository).delete(expired);
        verify(revokedSessionStore).revokeAfterCommit(sid, "refresh-expired");
    }

    @Test
    void revokesOrphanedSessionWhenAdminNoLongerExists() {
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        RefreshToken session = sessionOf(current, "fp");
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.of(session));
        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenSessionService.rotate(current.refreshToken(), "fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADMIN_NOT_FOUND_FOR_ME));

        verify(refreshTokenRepository).delete(session);
        verify(revokedSessionStore).revokeAfterCommit(sid, "admin-not-found");
    }

    @Test
    void revokesSessionWhenAdminIsInactive() {
        Admin admin = activeAdmin();
        when(admin.getStatus()).thenReturn(AdminStatus.INACTIVE);
        String sid = UUID.randomUUID().toString();
        JwtProvider.TokenPair current = jwtProvider.generateTokenPair(1L, "admin@example.com", "ADMIN", sid, "fp");
        RefreshToken session = sessionOf(current, "fp");
        when(refreshTokenRepository.findBySessionIdForUpdate(sid)).thenReturn(Optional.of(session));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> tokenSessionService.rotate(current.refreshToken(), "fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INACTIVE_ADMIN));

        verify(refreshTokenRepository).delete(session);
        verify(revokedSessionStore).revokeAfterCommit(sid, "admin-inactive");
    }

    @Test
    void keepsInvalidRefreshTokenExceptionForMalformedToken() {
        assertThatThrownBy(() -> tokenSessionService.rotate("not-a-jwt", "fp"))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void logoutDeletesOnlyCurrentSidAndAddsRedisRevocation() {
        String sid = UUID.randomUUID().toString();

        tokenSessionService.revokeSession(1L, sid);

        verify(refreshTokenRepository).deleteBySessionIdAndAdminId(sid, 1L);
        verify(revokedSessionStore).revokeAfterCommit(sid, "logout");
    }

    private RefreshToken sessionOf(JwtProvider.TokenPair pair, String fingerprint) {
        return RefreshToken.builder()
                .adminId(1L)
                .sessionId(pair.sessionId())
                .refreshJti(pair.refreshTokenId())
                .tokenHash(tokenHashService.hash(pair.refreshToken()))
                .fingerprintHash(fingerprint)
                .expiresAt(pair.refreshExpiresAt())
                .build();
    }

    private Admin activeAdmin() {
        Admin admin = mock(Admin.class);
        lenient().when(admin.getId()).thenReturn(1L);
        lenient().when(admin.getEmail()).thenReturn("admin@example.com");
        lenient().when(admin.getRole()).thenReturn(AdminRole.ADMIN);
        lenient().when(admin.getStatus()).thenReturn(AdminStatus.ACTIVE);
        return admin;
    }
}
