package org.backend.domain.auth.repository;

import org.backend.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByAdminId(Long adminId);
    void deleteByAdminId(Long adminId);
}