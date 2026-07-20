package org.backend.config.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.backend.config.security.JwtProvider;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.security.AdminPrincipal;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.backend.domain.auth.service.RevokedSessionStore;
import org.backend.domain.auth.service.TokenFingerprintService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedSessionStore revokedSessionStore;
    private final TokenFingerprintService tokenFingerprintService;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            AdminRepository adminRepository,
            RefreshTokenRepository refreshTokenRepository,
            RevokedSessionStore revokedSessionStore,
            TokenFingerprintService tokenFingerprintService
    ) {
        this.jwtProvider = jwtProvider;
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedSessionStore = revokedSessionStore;
        this.tokenFingerprintService = tokenFingerprintService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtProvider.validate(token) && jwtProvider.isAccessToken(token)) {
            Claims claims = jwtProvider.getClaims(token);

            String adminIdStr = claims.getSubject();
            String email = (String) claims.get("email");
            String role = (String) claims.get("role");
            String sessionId = claims.get("sid", String.class);
            String tokenId = claims.getId();
            String tokenFingerprint = claims.get("fp", String.class);
            String requestFingerprint = tokenFingerprintService.from(request);

            // ✅ DB status 체크: 비활성이면 인증 세팅하지 않음 (즉시 차단)
            if (StringUtils.hasText(adminIdStr)
                    && StringUtils.hasText(sessionId)
                    && StringUtils.hasText(tokenId)
                    && tokenFingerprintService.matches(requestFingerprint, tokenFingerprint)
                    && !revokedSessionStore.isRevoked(sessionId)) {
                Long adminId = Long.parseLong(adminIdStr);

                Admin admin = adminRepository.findById(adminId).orElse(null);
                boolean activeSession = refreshTokenRepository.existsBySessionIdAndAdminId(sessionId, adminId);
                if (admin != null && admin.getStatus() == AdminStatus.ACTIVE && activeSession) {
                    AdminPrincipal principal = new AdminPrincipal(adminId, email, role, sessionId, tokenId);
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearer)) return null;
        if (!bearer.startsWith("Bearer ")) return null;
        return bearer.substring(7);
    }
}
