CREATE DATABASE IF NOT EXISTS `${analysisSchema}`;

CREATE TABLE IF NOT EXISTS `${analysisSchema}`.churn_inference_batch (
    batch_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    target_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    model_version VARCHAR(64) NULL,
    snapshot_version VARCHAR(64) NULL,
    data_as_of DATETIME(6) NULL,
    error_message TEXT NULL,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `${analysisSchema}`.churn_prediction_staging (
    batch_id VARCHAR(36) NOT NULL,
    member_id BIGINT NOT NULL,
    churn_score DOUBLE NULL,
    risk_grade VARCHAR(20) NULL,
    model_version VARCHAR(64) NOT NULL,
    snapshot_version VARCHAR(64) NOT NULL,
    data_as_of DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (batch_id, member_id),
    KEY idx_churn_staging_status (batch_id, status, member_id),
    KEY idx_churn_staging_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `${analysisSchema}`.churn_prediction_current (
    member_id BIGINT NOT NULL,
    churn_score DOUBLE NOT NULL,
    risk_grade VARCHAR(20) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    snapshot_version VARCHAR(64) NOT NULL,
    data_as_of DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (member_id),
    KEY idx_churn_current_risk (risk_grade, member_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `${analysisSchema}`.churn_prediction_history (
    snapshot_version VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    churn_score DOUBLE NOT NULL,
    risk_grade VARCHAR(20) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    data_as_of DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (snapshot_version, member_id),
    KEY idx_churn_history_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `${analysisSchema}`.churn_prediction_summary_current (
    risk_grade VARCHAR(20) NOT NULL,
    customer_count BIGINT NOT NULL,
    ratio DOUBLE NOT NULL,
    data_as_of DATETIME(6) NOT NULL,
    PRIMARY KEY (risk_grade)
) ENGINE=InnoDB;
