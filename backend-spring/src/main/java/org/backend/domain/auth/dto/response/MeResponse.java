package org.backend.domain.auth.dto.response;

public record MeResponse(
        Long id,
        String email,
        String role
) {
}