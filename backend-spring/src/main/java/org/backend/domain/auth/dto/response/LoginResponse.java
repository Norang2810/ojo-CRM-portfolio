package org.backend.domain.auth.dto.response;

public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    private Long adminId;
    private String email;
    private String role;
    private String name;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String refreshToken, Long adminId, String email, String role, String name) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.adminId = adminId;
        this.email = email;
        this.role = role;
        this.name = name;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public Long getAdminId() { return adminId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getName() { return name; }

    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setName(String name) { this.name = name; }
}