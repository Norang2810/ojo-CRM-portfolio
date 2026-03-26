package org.backend.domain.auth.repository;

import org.backend.domain.auth.entity.AuthWhitelistEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthWhitelistEmailRepository extends JpaRepository<AuthWhitelistEmail, Long> {
    boolean existsByEmail(String email);
}