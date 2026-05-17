-- Dashboard aggregation optimization indexes
-- Apply before measuring 2nd-phase improvements.

CREATE INDEX idx_member_created_status
    ON member (created_at, status);

CREATE INDEX idx_analysis_member_created
    ON analysis (member_id, created_at);

CREATE INDEX idx_analysis_type_created_member
    ON analysis (type, created_at, member_id);

CREATE INDEX idx_lifecycle_date_dormant_member
    ON feature_lifecycle (feature_base_date, is_dormant_flag, member_id);
