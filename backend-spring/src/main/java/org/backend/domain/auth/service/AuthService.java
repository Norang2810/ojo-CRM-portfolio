package org.backend.domain.auth.service;

import io.jsonwebtoken.Claims;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.dto.request.LoginRequest;
import org.backend.domain.auth.dto.request.RefreshRequest;
import org.backend.domain.auth.dto.response.LoginResponse;
import org.backend.domain.auth.dto.response.MeResponse;
import org.backend.domain.auth.dto.response.TokenResponse;
import org.backend.domain.auth.entity.RefreshToken;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(AdminRepository adminRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new CustomException(ErrorCode.INACTIVE_ADMIN);
        }

        if (Boolean.TRUE.equals(admin.getGoogle())) {
            throw new CustomException(ErrorCode.GOOGLE_LOGIN_REQUIRED);
        }

        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_SET);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String role = admin.getRole().name();
        String accessToken = jwtProvider.generateAccessToken(admin.getId(), admin.getEmail(), role);
        String refreshToken = jwtProvider.generateRefreshToken(admin.getId());

        upsertRefreshToken(admin.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken, admin.getId(), admin.getEmail(), role, admin.getName());
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        return new MeResponse(admin.getId(), admin.getEmail(), admin.getRole().name());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validate(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Claims claims = jwtProvider.getClaims(refreshToken);
        Long adminId = Long.parseLong(claims.getSubject());

        RefreshToken saved = refreshTokenRepository.findByAdminId(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED));

        if (!refreshToken.equals(saved.getToken())) {
            refreshTokenRepository.delete(saved);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND_FOR_ME));

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            refreshTokenRepository.deleteByAdminId(adminId);
            throw new CustomException(ErrorCode.INACTIVE_ADMIN);
        }

        String role = admin.getRole().name();
        String newAccess = jwtProvider.generateAccessToken(admin.getId(), admin.getEmail(), role);
        String newRefresh = jwtProvider.generateRefreshToken(admin.getId());

        upsertRefreshToken(admin.getId(), newRefresh);

        return new TokenResponse(newAccess, newRefresh);
    }

    @Transactional
    public void logout(Long adminId) {
        refreshTokenRepository.deleteByAdminId(adminId);
    }

    private void upsertRefreshToken(Long adminId, String token) {
        RefreshToken rt = refreshTokenRepository.findByAdminId(adminId)
                .orElseGet(() -> RefreshToken.builder().adminId(adminId).build());

        rt.updateToken(token);
        refreshTokenRepository.save(rt);
    }
}