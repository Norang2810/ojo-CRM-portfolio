package org.backend.domain.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AdminPrincipal implements UserDetails {

    private final Long adminId;
    private final String email;
    private final String role; // "CS" | "MARKETING" | "ADMIN"

    public AdminPrincipal(Long adminId, String email, String role) {
        this.adminId = adminId;
        this.email = email;
        this.role = role;
    }

    public Long getAdminId() {
        return adminId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) return List.of();

        // role은 "ADMIN" 같은 값으로 들어오고,
        // Security 권한은 "ROLE_ADMIN" 형태로 맞춘다.
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return null; // JWT 방식이라 password는 principal에서 사용하지 않음
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}