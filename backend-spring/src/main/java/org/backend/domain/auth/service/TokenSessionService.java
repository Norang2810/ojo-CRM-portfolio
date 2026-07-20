package org.backend.domain.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.entity.RefreshToken;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminRepository adminRepository;
    private final JwtProvider jwtProvider;
    private final TokenHashService tokenHashService;
    private final TokenFingerprintService tokenFingerprintService;
    private final RevokedSessionStore revokedSessionStore;

    @Transactional
    public JwtProvider.TokenPair createSession(Admin admin, String fingerprint) {
        String sessionId = UUID.randomUUID().toString();
        JwtProvider.TokenPair pair = jwtProvider.generateTokenPair(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                sessionId,
                fingerprint
        );

        RefreshToken session = RefreshToken.builder()
                .adminId(admin.getId())
                .sessionId(pair.sessionId())
                .refreshJti(pair.refreshTokenId())
                .tokenHash(tokenHashService.hash(pair.refreshToken()))
                .fingerprintHash(fingerprint)
                .expiresAt(pair.refreshExpiresAt())
                .build();
        refreshTokenRepository.save(session);

        return pair;
    }

    /**
     * 예상된 인증 예외로 세션을 폐기한 경우에도 삭제가 커밋되어야 한다.
     * PESSIMISTIC_WRITE 잠금으로 동일 sid의 동시 Rotation을 직렬화한다.
     */
    @Transactional(noRollbackFor = CustomException.class)
    public JwtProvider.TokenPair rotate(String refreshToken, String requestFingerprint) {
        Claims claims = parseRefreshClaims(refreshToken);
        Long adminId = parseAdminId(claims);
        String sessionId = requiredClaim(claims, "sid");
        String refreshJti = claims.getId();
        if (!StringUtils.hasText(refreshJti)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken session = refreshTokenRepository.findBySessionIdForUpdate(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED));

        if (!adminId.equals(session.getAdminId())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String tokenFingerprint = requiredClaim(claims, "fp");
        boolean sameEnvironment = tokenFingerprintService.matches(requestFingerprint, tokenFingerprint)
                && tokenFingerprintService.matches(requestFingerprint, session.getFingerprintHash());
        if (!sameEnvironment) {
            revoke(session, "fingerprint-mismatch");
            throw new CustomException(ErrorCode.TOKEN_ENVIRONMENT_MISMATCH);
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            revoke(session, "refresh-expired");
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        boolean sameJti = refreshJti.equals(session.getRefreshJti());
        boolean sameToken = tokenHashService.matches(refreshToken, session.getTokenHash());
        if (!sameJti || !sameToken) {
            // 이미 교체된 RT의 재사용으로 간주하고 해당 토큰 패밀리(sid)를 폐기한다.
            revoke(session, "refresh-reuse");
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        Admin admin = adminRepository.findById(adminId)
                .orElse(null);
        if (admin == null) {
            revoke(session, "admin-not-found");
            throw new CustomException(ErrorCode.ADMIN_NOT_FOUND_FOR_ME);
        }
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            revoke(session, "admin-inactive");
            throw new CustomException(ErrorCode.INACTIVE_ADMIN);
        }

        JwtProvider.TokenPair rotated = jwtProvider.generateTokenPair(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                sessionId,
                requestFingerprint
        );
        session.rotate(
                rotated.refreshTokenId(),
                tokenHashService.hash(rotated.refreshToken()),
                rotated.refreshExpiresAt()
        );

        return rotated;
    }

    @Transactional
    public void revokeSession(Long adminId, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        refreshTokenRepository.deleteBySessionIdAndAdminId(sessionId, adminId);
        revokedSessionStore.revokeAfterCommit(sessionId, "logout");
    }

    @Transactional
    public void revokeAllSessions(Long adminId, String reason) {
        var sessionIds = refreshTokenRepository.findSessionIdsByAdminId(adminId);
        refreshTokenRepository.deleteByAdminId(adminId);
        sessionIds.forEach(sessionId -> revokedSessionStore.revokeAfterCommit(sessionId, reason));
    }

    private Claims parseRefreshClaims(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)
                || !jwtProvider.validate(refreshToken)
                || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return jwtProvider.getClaims(refreshToken);
    }

    private Long parseAdminId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private String requiredClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return String.valueOf(value);
    }

    private void revoke(RefreshToken session, String reason) {
        refreshTokenRepository.delete(session);
        revokedSessionStore.revokeAfterCommit(session.getSessionId(), reason);
    }
}
