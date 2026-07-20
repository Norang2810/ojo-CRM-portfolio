# Analytics pipeline local baseline (2026-07-18)

This baseline records only values verified from the local Docker MySQL instance. It is not a production benchmark.

## Verified row counts

| Table | Rows |
|---|---:|
| `member` | 100,000 |
| `advice` | 0 |
| `invoice` | 0 |
| `payment` | 0 |
| `data_usage` | 0 |
| `subscription_period` | 0 |
| `feature_consultation` | 100,000 |
| `feature_lifecycle` | 800,000 |
| `feature_monetary` | 0 |
| `feature_usage` | 0 |

## Existing batch metadata

- `BATCH_JOB_EXECUTION`: 3 executions
- Completed executions: 0
- Failed executions: 3
- Recorded execution windows: 0 seconds, 0 seconds, and 8 seconds before failure

The current data cannot establish a valid Full pipeline duration, inference throughput, or API performance baseline because the payment/usage sources are empty and no historical Job completed successfully. The 20-minute incremental and 6-hour Full acceptance criteria therefore remain load-test gates, not measured achievements.

## Environment checks completed

- Flyway V1-V3 succeeded on both the existing schema and a clean temporary schema.
- The source `updated_at` composite indexes and member status-history trigger exist in MySQL.
- Python MySQL sessions report `@@session.time_zone = '+00:00'` and `NOW() = UTC_TIMESTAMP()`.
- No prior “99% response reduction” or “66x TPS” statement is reused as a baseline.
