CREATE TABLE IF NOT EXISTS refresh_token_sessions (
    refresh_token_session_id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    refresh_jti VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    fingerprint_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (refresh_token_session_id),
    CONSTRAINT uk_refresh_token_sessions_sid UNIQUE (session_id),
    INDEX idx_refresh_token_sessions_admin_id (admin_id),
    INDEX idx_refresh_token_sessions_expires_at (expires_at)
);

-- Legacy tokens do not contain jti/sid/fingerprint and cannot be safely migrated.
-- Dropping the old plaintext-token table intentionally requires one re-login at rollout.
DROP TABLE IF EXISTS refresh_tokens;
