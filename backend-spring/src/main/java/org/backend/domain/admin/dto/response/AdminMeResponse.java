package org.backend.domain.admin.dto.response;

import org.backend.domain.admin.entity.Admin;
import org.backend.domain.admin.entity.AdminStatus;

public class AdminMeResponse {

    private Long adminId;
    private String name;
    private String email;
    private String phone;
    private Boolean google;
    private String role; // "CS" | "MARKETING" | "ADMIN"
    private AdminStatus status;

    public AdminMeResponse() {}

    public AdminMeResponse(Long adminId, String name, String email, String phone,
                           Boolean google, String role, AdminStatus status) {
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.google = google;
        this.role = role;
        this.status = status;
    }

    public static AdminMeResponse from(Admin admin) {
        return new AdminMeResponse(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPhone(),
                admin.getGoogle(),
                admin.getRole().name(),
                admin.getStatus()
        );
    }

    public Long getAdminId() { return adminId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Boolean getGoogle() { return google; }
    public String getRole() { return role; }
    public AdminStatus getStatus() { return status; }

    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGoogle(Boolean google) { this.google = google; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(AdminStatus status) { this.status = status; }
}