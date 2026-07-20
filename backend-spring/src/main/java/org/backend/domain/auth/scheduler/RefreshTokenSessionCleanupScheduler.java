package org.backend.domain.auth.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.auth.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenSessionCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.jwt.refresh-session-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredSessions() {
        long deleted = refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Deleted expired refresh token sessions: count={}", deleted);
        }
    }
}
