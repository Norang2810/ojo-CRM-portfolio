package org.backend.domain.admin.dto.response;

import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;

import java.time.LocalDateTime;

public record AdminSummaryDto(
        Long adminId,
        String name,
        String email,
        String phone,
        Boolean google,
        String role,
        AdminStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminSummaryDto from(Admin a) {
        return new AdminSummaryDto(
                a.getId(),
                a.getName(),
                a.getEmail(),
                a.getPhone(),
                a.getGoogle(),
                a.getRole().name(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}