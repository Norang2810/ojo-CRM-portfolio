// backend-spring/src/main/java/org/backend/config/security/filter/AccessLogFilter.java
package org.backend.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.backend.domain.auth.security.AdminPrincipal;
import org.backend.domain.auth.service.AccessLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private final AccessLogService accessLogService;

    public AccessLogFilter(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                Long userId = null;
                String email = null;
                String role = null;

                if (auth != null && auth.getPrincipal() != null) {
                    Object principal = auth.getPrincipal();

                    //  AdminPrincipal 기반으로 다 뽑는다
                    if (principal instanceof AdminPrincipal ap) {
                        userId = ap.getAdminId();
                        email = ap.getUsername(); // username == email
                        role = ap.getRole();
                    } else if (principal instanceof String s) {
                        // (방어) 예전 방식
                        try { userId = Long.parseLong(s); } catch (Exception ignored) {}
                        if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                            role = auth.getAuthorities().iterator().next().getAuthority();
                        }
                    } else {
                        if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                            role = auth.getAuthorities().iterator().next().getAuthority();
                        }
                    }
                }

                accessLogService.log(
                        request,
                        userId,
                        email,
                        role,
                        response.getStatus()
                );
            } catch (Exception ignored) {
                // 로깅 실패가 서비스 실패로 번지면 안 됨
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 필요하면 제외 패턴 추가 가능 (ex: health, swagger 등)
        return false;
    }
}