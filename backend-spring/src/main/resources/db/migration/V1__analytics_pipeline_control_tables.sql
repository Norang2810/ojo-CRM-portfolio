CREATE TABLE IF NOT EXISTS analytics_batch_run (
    batch_id VARCHAR(36) NOT NULL,
    run_type VARCHAR(20) NOT NULL,
    feature_base_date DATE NOT NULL,
    feature_base_at DATETIME(6) NOT NULL,
    window_start_at DATETIME(6) NOT NULL,
    window_end_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    target_count BIGINT NOT NULL DEFAULT 0,
    feature_success_count BIGINT NOT NULL DEFAULT 0,
    inference_success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    model_version VARCHAR(64) NULL,
    snapshot_version VARCHAR(64) NULL,
    error_message TEXT NULL,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id),
    UNIQUE KEY uk_analytics_batch_window (run_type, window_start_at, window_end_at),
    KEY idx_analytics_batch_status_started (status, started_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS analytics_batch_target (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    inference_required BOOLEAN NOT NULL DEFAULT TRUE,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_analytics_target_status (batch_id, status, member_id),
    KEY idx_analytics_target_member (member_id, batch_id),
    CONSTRAINT fk_analytics_target_run FOREIGN KEY (batch_id)
        REFERENCES analytics_batch_run (batch_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS analytics_watermark (
    pipeline_name VARCHAR(100) NOT NULL,
    last_successful_end_at DATETIME(6) NOT NULL,
    batch_id VARCHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (pipeline_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS analytics_snapshot_manifest (
    snapshot_version VARCHAR(64) NOT NULL,
    batch_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_as_of DATETIME(6) NOT NULL,
    model_version VARCHAR(64) NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    activated_at DATETIME(6) NULL,
    PRIMARY KEY (snapshot_version),
    UNIQUE KEY uk_analytics_snapshot_batch (batch_id),
    KEY idx_analytics_snapshot_active (active, activated_at),
    CONSTRAINT fk_analytics_manifest_run FOREIGN KEY (batch_id)
        REFERENCES analytics_batch_run (batch_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS member_status_history (
    history_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'APPLICATION',
    batch_id VARCHAR(36) NULL,
    PRIMARY KEY (history_id),
    KEY idx_member_status_changed (changed_at, member_id),
    KEY idx_member_status_member_changed (member_id, changed_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feature_consultation_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    feature_base_date DATE NOT NULL,
    feature_base_at DATETIME(6) NOT NULL,
    total_consult_count INT NOT NULL,
    last_7d_consult_count INT NOT NULL,
    last_30d_consult_count INT NOT NULL,
    avg_monthly_consult_count FLOAT NOT NULL,
    last_consult_date DATE NULL,
    top_consult_category VARCHAR(255) NULL,
    total_complaint_count INT NOT NULL,
    last_consult_days_ago INT NULL,
    night_consult_count INT NULL,
    weekend_consult_count INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_feature_consultation_staging_base (feature_base_date, member_id),
    KEY idx_feature_consultation_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feature_lifecycle_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    feature_base_date DATE NOT NULL,
    feature_base_at DATETIME(6) NOT NULL,
    signup_date DATE NOT NULL,
    member_lifetime_days INT NOT NULL,
    is_new_customer_flag CHAR(1) NOT NULL,
    is_dormant_flag CHAR(1) NOT NULL,
    is_terminated_flag CHAR(1) NOT NULL,
    days_since_last_activity INT NULL,
    contract_end_days_left INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_feature_lifecycle_staging_base (feature_base_date, member_id),
    KEY idx_feature_lifecycle_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feature_monetary_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    feature_base_date DATE NOT NULL,
    feature_base_at DATETIME(6) NOT NULL,
    total_revenue BIGINT NOT NULL,
    last_payment_amount BIGINT NOT NULL,
    avg_monthly_bill FLOAT NOT NULL,
    last_payment_date DATE NULL,
    payment_count_6m INT NOT NULL,
    monthly_revenue BIGINT NOT NULL,
    payment_delay_count INT NOT NULL,
    prev_monthly_revenue BIGINT NOT NULL,
    purchase_cycle INT NOT NULL,
    is_vip_prev_month CHAR(1) NOT NULL,
    avg_order_val FLOAT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_feature_monetary_staging_base (feature_base_date, member_id),
    KEY idx_feature_monetary_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feature_usage_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    feature_base_date DATE NOT NULL,
    feature_base_at DATETIME(6) NOT NULL,
    total_usage_amount BIGINT NOT NULL,
    avg_daily_usage FLOAT NOT NULL,
    max_usage_amount BIGINT NOT NULL,
    usage_peak_hour INT NULL,
    premium_service_count INT NOT NULL,
    last_activity_date DATE NULL,
    usage_active_days_30d INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_feature_usage_staging_base (feature_base_date, member_id),
    KEY idx_feature_usage_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rfm_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    recency DATETIME(6) NULL,
    frequency INT NULL,
    monetary BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_rfm_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS analysis_current (
    member_id BIGINT NOT NULL,
    snapshot_version VARCHAR(64) NOT NULL,
    rfm_score INT NULL,
    type VARCHAR(255) NULL,
    ltv BIGINT NULL,
    lifecycle_stage VARCHAR(255) NULL,
    r_score INT NULL,
    f_score INT NULL,
    m_score INT NULL,
    data_as_of DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (member_id),
    KEY idx_analysis_current_type (type, member_id),
    KEY idx_analysis_current_snapshot (snapshot_version, member_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS analysis_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    snapshot_version VARCHAR(64) NOT NULL,
    rfm_score INT NULL,
    type VARCHAR(255) NULL,
    ltv BIGINT NULL,
    lifecycle_stage VARCHAR(255) NULL,
    r_score INT NULL,
    f_score INT NULL,
    m_score INT NULL,
    data_as_of DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_analysis_staging_snapshot (snapshot_version, member_id),
    KEY idx_analysis_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS analysis_history (
    snapshot_version VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    rfm_score INT NULL,
    type VARCHAR(255) NULL,
    ltv BIGINT NULL,
    lifecycle_stage VARCHAR(255) NULL,
    r_score INT NULL,
    f_score INT NULL,
    m_score INT NULL,
    data_as_of DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (snapshot_version, member_id),
    KEY idx_analysis_history_created (created_at)
) ENGINE=InnoDB;
