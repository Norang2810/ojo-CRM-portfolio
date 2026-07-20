package org.backend.domain.auth.repository;

import org.backend.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.sessionId = :sessionId")
    Optional<RefreshToken> findBySessionIdForUpdate(@Param("sessionId") String sessionId);

    boolean existsBySessionIdAndAdminId(String sessionId, Long adminId);

    long deleteBySessionIdAndAdminId(String sessionId, Long adminId);

    @Query("select rt.sessionId from RefreshToken rt where rt.adminId = :adminId")
    List<String> findSessionIdsByAdminId(@Param("adminId") Long adminId);

    void deleteByAdminId(Long adminId);

    long deleteByExpiresAtBefore(Instant expiresAt);
}
