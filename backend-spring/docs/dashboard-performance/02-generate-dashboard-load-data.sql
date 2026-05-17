-- Adjust @member_count to scale the experiment.
SET @member_count = 100000;
SET SESSION cte_max_recursion_depth = 100000;

WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @member_count
)
INSERT INTO member (
    name, phone, email, gender, birth_date, region, address, household_type, created_at, status
)
SELECT
    CONCAT('dashboard_member_', n),
    CONCAT('010-', LPAD(n % 10000, 4, '0'), '-', LPAD((n * 7) % 10000, 4, '0')),
    CONCAT('dashboard_', n, '@example.com'),
    IF(n % 2 = 0, 'M', 'F'),
    DATE_SUB(CURRENT_DATE, INTERVAL (7000 + n % 8000) DAY),
    'Seoul',
    CONCAT('Seoul ', n),
    1 + (n % 4),
    DATE_SUB(NOW(), INTERVAL (n % 365) DAY),
    CASE
        WHEN n % 20 = 0 THEN 'TERMINATED'
        WHEN n % 11 = 0 THEN 'DORMANT'
        ELSE 'ACTIVE'
    END
FROM seq;

INSERT INTO analysis (member_id, rfm_score, type, ltv, lifecycle_stage, created_at)
SELECT
    m.member_id,
    100 + (m.member_id % 900),
    CASE
        WHEN m.member_id % 10 = 0 THEN 'VIP'
        WHEN m.member_id % 10 IN (1, 2) THEN 'LOYAL'
        WHEN m.member_id % 10 IN (3, 4) THEN 'RISK'
        WHEN m.member_id % 10 = 5 THEN 'SLEEP'
        WHEN m.member_id % 10 = 6 THEN 'LOST'
        ELSE 'COMMON'
    END,
    10000 + (m.member_id % 500000),
    'Stage',
    DATE_SUB(NOW(), INTERVAL (m.member_id % 30) DAY)
FROM member m
WHERE m.name LIKE 'dashboard_member_%';

INSERT INTO feature_lifecycle (
    member_id, feature_base_date, member_lifetime_days, days_since_last_activity,
    contract_end_days_left, is_dormant_flag, is_new_customer_flag, is_terminated_flag, signup_date
)
SELECT
    m.member_id,
    d.feature_base_date,
    365,
    m.member_id % 60,
    30,
    CASE WHEN (m.member_id + d.offset_day) % 9 = 0 THEN 'Y' ELSE 'N' END,
    CASE WHEN d.offset_day = 0 AND m.member_id % 15 = 0 THEN 'Y' ELSE 'N' END,
    CASE WHEN m.status = 'TERMINATED' THEN 'Y' ELSE 'N' END,
    DATE_SUB(CURRENT_DATE, INTERVAL 365 DAY)
FROM member m
JOIN (
    SELECT CURRENT_DATE - INTERVAL 7 DAY AS feature_base_date, 7 AS offset_day
    UNION ALL SELECT CURRENT_DATE - INTERVAL 6 DAY, 6
    UNION ALL SELECT CURRENT_DATE - INTERVAL 5 DAY, 5
    UNION ALL SELECT CURRENT_DATE - INTERVAL 4 DAY, 4
    UNION ALL SELECT CURRENT_DATE - INTERVAL 3 DAY, 3
    UNION ALL SELECT CURRENT_DATE - INTERVAL 2 DAY, 2
    UNION ALL SELECT CURRENT_DATE - INTERVAL 1 DAY, 1
    UNION ALL SELECT CURRENT_DATE, 0
) d
WHERE m.name LIKE 'dashboard_member_%';
