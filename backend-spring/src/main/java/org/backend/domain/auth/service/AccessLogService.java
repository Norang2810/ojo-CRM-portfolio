package org.backend.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.backend.domain.auth.dto.response.AccessLogSummaryDto;
import org.backend.domain.auth.entity.AccessLog;
import org.backend.domain.auth.repository.AccessLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public AccessLogService(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    /**
     * 메인 트랜잭션이 실패해도 로그는 남기기 위해 REQUIRES_NEW
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(HttpServletRequest request, Long userId, String email, String role, int statusCode) {
        try {
            AccessLog log = AccessLog.builder()
                    .userId(userId)
                    .email(email)
                    .role(role)
                    .method(request.getMethod())
                    .path(request.getRequestURI())
                    .statusCode(statusCode)
                    .ip(extractClientIp(request))
                    .userAgent(trim(request.getHeader("User-Agent"), 255))
                    .build();

            accessLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    private String trim(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xrip = request.getHeader("X-Real-IP");
        if (xrip != null && !xrip.isBlank()) return xrip.trim();

        return request.getRemoteAddr();
    }

    @Transactional(readOnly = true)
    public Page<AccessLogSummaryDto> getAccessLogs(Integer page, Integer size) {
        int p = (page == null) ? 0 : Math.max(page, 0);
        int s = (size == null) ? 20 : Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findAll(pageable).map(AccessLogSummaryDto::from);
    }
}