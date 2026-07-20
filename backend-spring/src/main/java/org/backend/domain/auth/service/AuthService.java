package org.backend.domain.auth.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;

    public AuthService(AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder,
                       TokenSessionService tokenSessionService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenSessionService = tokenSessionService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String fingerprint) {
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
        JwtProvider.TokenPair pair = tokenSessionService.createSession(admin, fingerprint);

        return new LoginResponse(
                pair.accessToken(),
                pair.refreshToken(),
                admin.getId(),
                admin.getEmail(),
                role,
                admin.getName()
        );
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        return new MeResponse(admin.getId(), admin.getEmail(), admin.getRole().name());
    }

    public TokenResponse refresh(RefreshRequest request, String fingerprint) {
        JwtProvider.TokenPair pair = tokenSessionService.rotate(request.getRefreshToken(), fingerprint);
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @Transactional
    public void logout(Long adminId, String sessionId) {
        tokenSessionService.revokeSession(adminId, sessionId);
    }
}
