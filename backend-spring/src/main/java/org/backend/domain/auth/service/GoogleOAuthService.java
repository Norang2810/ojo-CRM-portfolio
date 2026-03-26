package org.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.dto.response.LoginResponse;
import org.backend.domain.auth.entity.RefreshToken;
import org.backend.domain.auth.oauth.GoogleOAuthClient;
import org.backend.domain.auth.oauth.dto.GoogleUserInfo;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final WhitelistService whitelistService;

    private final AdminRepository adminRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse loginWithCode(String code) {
        GoogleUserInfo userInfo = googleOAuthClient.getUserInfoByCode(code);

        whitelistService.validateAndAutoRegister(userInfo.email());

        Admin admin = adminRepository.findByEmail(userInfo.email())
                .orElseGet(() -> adminRepository.save(Admin.createGoogleUser(userInfo.name(), userInfo.email())));

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            refreshTokenRepository.deleteByAdminId(admin.getId());
            throw new CustomException(ErrorCode.INACTIVE_ADMIN);
        }

        String accessToken = jwtProvider.generateAccessToken(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name()
        );
        String refreshToken = jwtProvider.generateRefreshToken(admin.getId());

        upsertRefreshToken(admin.getId(), refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken,
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                admin.getName()
        );
    }

    private void upsertRefreshToken(Long adminId, String token) {
        RefreshToken rt = refreshTokenRepository.findByAdminId(adminId)
                .orElseGet(() -> RefreshToken.builder().adminId(adminId).build());

        rt.updateToken(token);
        refreshTokenRepository.save(rt);
    }
}