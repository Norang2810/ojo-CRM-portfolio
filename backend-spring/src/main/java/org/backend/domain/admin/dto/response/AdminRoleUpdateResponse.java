package org.backend.domain.admin.dto.response;

import org.backend.domain.admin.entity.AdminRole;

public record AdminRoleUpdateResponse(Long adminId, AdminRole role) {
    public static AdminRoleUpdateResponse of(Long adminId, AdminRole role) {
        return new AdminRoleUpdateResponse(adminId, role);
    }
}