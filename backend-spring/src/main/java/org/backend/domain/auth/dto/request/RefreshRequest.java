package org.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequest {

    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;

    public RefreshRequest() {}

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}