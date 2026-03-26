package org.backend.domain.auth.dto.response;

import org.backend.domain.auth.entity.AccessLog;

import java.time.LocalDateTime;

public record AccessLogSummaryDto(
        Long id,
        LocalDateTime createdAt,
        Long userId,
        String email,
        String role,
        String method,
        String path,
        Integer statusCode,
        String ip
) {
    public static AccessLogSummaryDto from(AccessLog log) {
        return new AccessLogSummaryDto(
                log.getId(),
                log.getCreatedAt(),
                log.getUserId(),
                log.getEmail(),
                log.getRole(),
                log.getMethod(),
                log.getPath(),
                log.getStatusCode(),
                log.getIp()
        );
    }
}