# Dashboard Performance Improvement Narrative & Measurement Guide

## Goal
Use one reproducible dataset to document a three-step dashboard optimization story:

1. Remove request-time aggregation bottlenecks.
2. Optimize the new batch aggregation cost created by step 1.
3. Improve cache refresh stability after batch completion.

## Naming Convention
- `저부하 baseline`
  - Threads: 10
  - Ramp-up: 10
  - Loop: 10
- `고부하 baseline`
  - Threads: 50
  - Ramp-up: 25
  - Loop: 20

Do not use `1차 baseline` or `2차 baseline` in portfolio text because those names conflict with the later `2차 개선` and `3차 개선`.

## Phase 1: Remove request-time aggregation bottlenecks

### Problem
- The dashboard API recalculated summary statistics on every request.
- As load increased, repeated aggregation queries became the dominant response-time bottleneck.

### Baseline Evidence
| Scenario | Avg | p95 | Throughput | Error |
| --- | ---: | ---: | ---: | ---: |
| 저부하 baseline | about 6,078ms | - | about 1.4/sec | - |
| 고부하 baseline | about 19,740ms | about 35,101ms | about 1.2/sec | 0% |

### Improvement
- Replaced repeated real-time aggregation with precomputed summary tables.
- Added Redis caching for dashboard reads.

### Result
- Response time reduced by more than 99%.
- TPS improved by about 66x.
- The architecture changed from repeated request-time aggregation to precomputed reads.

### Portfolio-ready Summary
- `반복 실시간 집계가 발생하던 대시보드 조회를 선계산 테이블 + Redis 캐시 구조로 전환해 응답 시간을 99% 이상 단축하고 TPS를 약 66배 향상`

## Phase 2: Optimize the new batch aggregation cost

### Why this follows naturally after phase 1
- Phase 1 removed the user-facing lookup bottleneck, but the expensive aggregation work did not disappear.
- It moved into the batch pipeline, so the next optimization target became the cost of generating the precomputed data itself.

### Improvement
- Add composite indexes for dashboard aggregation predicates.
- Refine the latest-analysis aggregation query.
- Replace loosely typed map-based aggregation results with projection-based reads.

### Metrics to Record
- Total dashboard aggregation runtime.
- Execution time of key aggregation queries.
- Rows examined.
- `EXPLAIN ANALYZE` changes such as full scan, temporary table, and filesort usage.

### Measurement Procedure
1. Load the same synthetic dataset with `02-generate-dashboard-load-data.sql`.
2. Record aggregation runtime and `EXPLAIN ANALYZE` results before applying indexes.
3. Apply `01-dashboard-indexes.sql`.
4. Re-run the exact same measurements.

### Portfolio-ready Summary Template
- `선계산 배치 내부 집계 쿼리에 복합 인덱스와 Projection 기반 조회를 적용해 배치 집계 시간을 X초에서 Y초로 단축`

## Phase 3: Improve cache refresh stability

### Why this follows naturally after phase 2
- After lookup speed and batch cost were addressed, the remaining weak point was the transition moment after batch completion.
- If cache entries are removed before the new values are ready, the first user request after a batch may still pay the cache-miss cost.

### Improvement
- Keep the existing cache until the aggregation transaction succeeds.
- Clear cache only after commit.
- Warm the latest dashboard response immediately after a successful refresh.
- Preserve the previous cache entry if aggregation fails.

### Metrics to Record
- First-request latency after cache clear.
- First-request latency after warm-up.
- Cache hit/miss behavior.
- Whether the previous cache survives a failed aggregation.

### Measurement Procedure
1. Measure a cold-cache first request after manually clearing `dashboardCache`.
2. Run dashboard aggregation and measure the first request after warm-up.
3. Simulate an aggregation failure before commit and verify the previous cache entry remains available.

### Portfolio-ready Summary Template
- `배치 성공 후 캐시 warm-up과 실패 시 기존 캐시 유지 흐름을 적용해 배치 직후 첫 요청 지연을 Xms에서 Yms로 감소`

## Shared Setup
1. Load synthetic data with `02-generate-dashboard-load-data.sql`.
2. For phase 2, measure once before and once after applying `01-dashboard-indexes.sql`.
3. Flush Redis only when measuring cold-cache behavior.

## Suggested SQL
```sql
EXPLAIN ANALYZE
SELECT COUNT(*)
FROM member
WHERE created_at < CURRENT_DATE + INTERVAL 1 DAY
  AND status <> 'TERMINATED';

EXPLAIN ANALYZE
SELECT COUNT(*)
FROM analysis a
JOIN (
    SELECT member_id, MAX(created_at) AS max_date
    FROM analysis
    GROUP BY member_id
) latest
  ON a.member_id = latest.member_id
 AND a.created_at = latest.max_date
WHERE a.type IN ('RISK', 'SLEEP')
  AND a.created_at >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
  AND a.created_at < CURRENT_DATE + INTERVAL 1 DAY;
```
