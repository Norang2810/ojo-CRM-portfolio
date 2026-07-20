DROP PROCEDURE IF EXISTS analytics_add_column_if_missing;
DROP PROCEDURE IF EXISTS analytics_add_index_if_missing;

DELIMITER //
CREATE PROCEDURE analytics_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = p_table_name
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @analytics_ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD COLUMN `',
            p_column_name, '` ', p_definition
        );
        PREPARE analytics_statement FROM @analytics_ddl;
        EXECUTE analytics_statement;
        DEALLOCATE PREPARE analytics_statement;
    END IF;
END//

CREATE PROCEDURE analytics_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = p_table_name
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @analytics_ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD INDEX `',
            p_index_name, '` (', p_columns, ')'
        );
        PREPARE analytics_statement FROM @analytics_ddl;
        EXECUTE analytics_statement;
        DEALLOCATE PREPARE analytics_statement;
    END IF;
END//
DELIMITER ;

CALL analytics_add_column_if_missing('member', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('advice', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('invoice', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('payment', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('data_usage', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('subscription_period', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');

CALL analytics_add_index_if_missing('member', 'idx_member_updated_member', '`updated_at`, `member_id`');
CALL analytics_add_index_if_missing('advice', 'idx_advice_updated_member', '`updated_at`, `member_id`');
CALL analytics_add_index_if_missing('invoice', 'idx_invoice_updated_member', '`updated_at`, `member_id`');
CALL analytics_add_index_if_missing('payment', 'idx_payment_updated_invoice', '`updated_at`, `invoice_id`');
CALL analytics_add_index_if_missing('data_usage', 'idx_usage_updated_member', '`updated_at`, `member_id`');
CALL analytics_add_index_if_missing('subscription_period', 'idx_subscription_updated_member', '`updated_at`, `member_id`');

CALL analytics_add_column_if_missing('feature_consultation', 'feature_base_at', 'DATETIME(6) NULL');
CALL analytics_add_column_if_missing('feature_consultation', 'batch_id', 'VARCHAR(36) NULL');
CALL analytics_add_column_if_missing('feature_consultation', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('feature_lifecycle', 'feature_base_at', 'DATETIME(6) NULL');
CALL analytics_add_column_if_missing('feature_lifecycle', 'batch_id', 'VARCHAR(36) NULL');
CALL analytics_add_column_if_missing('feature_lifecycle', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('feature_monetary', 'feature_base_at', 'DATETIME(6) NULL');
CALL analytics_add_column_if_missing('feature_monetary', 'batch_id', 'VARCHAR(36) NULL');
CALL analytics_add_column_if_missing('feature_monetary', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL analytics_add_column_if_missing('feature_usage', 'feature_base_at', 'DATETIME(6) NULL');
CALL analytics_add_column_if_missing('feature_usage', 'batch_id', 'VARCHAR(36) NULL');
CALL analytics_add_column_if_missing('feature_usage', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');

CALL analytics_add_column_if_missing('dashboard_summary_stats', 'snapshot_version', 'VARCHAR(64) NULL');
CALL analytics_add_column_if_missing('dashboard_summary_stats', 'computed_at', 'DATETIME(6) NULL');
CALL analytics_add_column_if_missing('dashboard_daily_stats', 'snapshot_version', 'VARCHAR(64) NULL');
CALL analytics_add_column_if_missing('dashboard_daily_stats', 'computed_at', 'DATETIME(6) NULL');

DROP PROCEDURE analytics_add_column_if_missing;
DROP PROCEDURE analytics_add_index_if_missing;
