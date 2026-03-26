package org.backend.domain.auth.dto.response;

public class TokenResponse {

    private String accessToken;
    private String refreshToken;

    // 항상 Bearer 고정이면 상수처럼 두는 게 안전
    private final String tokenType = "Bearer";

    public TokenResponse() {}

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}