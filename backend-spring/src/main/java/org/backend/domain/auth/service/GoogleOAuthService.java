package org.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.dto.response.LoginResponse;
import org.backend.domain.auth.oauth.GoogleOAuthClient;
import org.backend.domain.auth.oauth.dto.GoogleUserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final WhitelistService whitelistService;

    private final AdminRepository adminRepository;
    private final TokenSessionService tokenSessionService;

    @Transactional(noRollbackFor = CustomException.class)
    public LoginResponse loginWithCode(String code, String fingerprint) {
        GoogleUserInfo userInfo = googleOAuthClient.getUserInfoByCode(code);

        whitelistService.validateAndAutoRegister(userInfo.email());

        Admin admin = adminRepository.findByEmail(userInfo.email())
                .orElseGet(() -> adminRepository.save(Admin.createGoogleUser(userInfo.name(), userInfo.email())));

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            tokenSessionService.revokeAllSessions(admin.getId(), "admin-inactive");
            throw new CustomException(ErrorCode.INACTIVE_ADMIN);
        }

        JwtProvider.TokenPair pair = tokenSessionService.createSession(admin, fingerprint);

        return new LoginResponse(
                pair.accessToken(),
                pair.refreshToken(),
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                admin.getName()
        );
    }
}
