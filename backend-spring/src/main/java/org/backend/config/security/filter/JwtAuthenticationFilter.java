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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AdminRepository adminRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, AdminRepository adminRepository) {
        this.jwtProvider = jwtProvider;
        this.adminRepository = adminRepository;
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

            // ✅ DB status 체크: 비활성이면 인증 세팅하지 않음 (즉시 차단)
            if (StringUtils.hasText(adminIdStr)) {
                Long adminId = Long.parseLong(adminIdStr);

                Admin admin = adminRepository.findById(adminId).orElse(null);
                if (admin != null && admin.getStatus() == AdminStatus.ACTIVE) {
                    AdminPrincipal principal = new AdminPrincipal(adminId, email, role);
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
