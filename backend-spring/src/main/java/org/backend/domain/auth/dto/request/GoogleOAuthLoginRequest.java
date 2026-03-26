package org.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleOAuthLoginRequest(
        @NotBlank String code
) {
}