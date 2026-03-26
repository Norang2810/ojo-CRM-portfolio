package org.backend.domain.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.auth.service.AccessLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

    private final AccessLogService accessLogService;
    private final AdminRepository adminRepository;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Long userId = null;
        String email = null;
        String role = "ROLE_ANONYMOUS";

        // JwtAuthenticationFilter에서 principal을 adminId(String)로 넣고 있었지?
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                userId = Long.parseLong(String.valueOf(auth.getPrincipal()));
                // email은 토큰에 넣어도 되지만, 지금은 DB에서 가져오는 방식으로 처리
                email = adminRepository.findById(userId).map(a -> a.getEmail()).orElse(null);

                // authority는 이미 ROLE_ 붙은 형태로 들어오도록 구현돼있음
                role = auth.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority())
                        .orElse(role);
            } catch (Exception ignored) {
            }
        }

        // 응답 statusCode는 response에서 확정값을 가져옴
        accessLogService.log(request, userId, email, role, response.getStatus());
    }
}