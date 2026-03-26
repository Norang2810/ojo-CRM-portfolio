package org.backend.domain.admin.dto.response;

import org.backend.domain.admin.entity.AdminStatus;

public record AdminStatusUpdateResponse(Long adminId, AdminStatus status) {
    public static AdminStatusUpdateResponse of(Long adminId, AdminStatus status) {
        return new AdminStatusUpdateResponse(adminId, status);
    }
}