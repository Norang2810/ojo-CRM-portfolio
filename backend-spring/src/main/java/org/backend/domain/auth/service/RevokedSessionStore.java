package org.backend.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Slf4j
@Component
public class RevokedSessionStore {

    private static final String KEY_PREFIX = "auth:revoked:sid:";

    private final StringRedisTemplate redisTemplate;
    private final Duration accessTokenValidity;

    public RevokedSessionStore(
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.access-token-validity:1h}") Duration accessTokenValidity
    ) {
        this.redisTemplate = redisTemplate;
        this.accessTokenValidity = accessTokenValidity;
    }

    public boolean isRevoked(String sessionId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(sessionId)));
        } catch (RuntimeException e) {
            // Redis 장애 시 DB 세션 존재 여부가 최종 검증을 담당한다.
            log.warn("Failed to read revoked JWT session from Redis: sid={}", sessionId, e);
            return false;
        }
    }

    public void revokeAfterCommit(String sessionId, String reason) {
        Runnable revoke = () -> revokeNow(sessionId, reason);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    revoke.run();
                }
            });
            return;
        }
        revoke.run();
    }

    private void revokeNow(String sessionId, String reason) {
        try {
            redisTemplate.opsForValue().set(key(sessionId), reason, accessTokenValidity);
        } catch (RuntimeException e) {
            // DB 행은 이미 삭제되므로 Redis 실패가 세션 폐기를 되돌리지는 않는다.
            log.warn("Failed to cache revoked JWT session in Redis: sid={}", sessionId, e);
        }
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
