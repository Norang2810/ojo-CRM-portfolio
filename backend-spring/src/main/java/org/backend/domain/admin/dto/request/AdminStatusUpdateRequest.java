package org.backend.domain.admin.dto.request;

import org.backend.domain.admin.entity.AdminStatus;

public record AdminStatusUpdateRequest(AdminStatus status) {
}