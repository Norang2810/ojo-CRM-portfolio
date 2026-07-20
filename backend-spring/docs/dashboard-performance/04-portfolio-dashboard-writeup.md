# Dashboard Performance Portfolio Write-up Draft

## 1차 개선 — 실시간 반복 집계 제거

### Problem
- 대시보드 조회 시마다 실시간 집계 쿼리를 반복 수행해, 부하 증가에 따라 응답 시간이 급격히 악화됐다.

### Evidence
- 저부하 baseline: 평균 응답 시간 약 6,078ms, Throughput 약 1.4/sec
- 고부하 baseline: 평균 응답 시간 약 19,740ms, p95 약 35,101ms, Throughput 약 1.2/sec, Error 0%

### Action
- 반복 실시간 집계를 제거하고, 선계산 테이블 + Redis 캐시 구조로 전환했다.

### Result
- 응답 시간 99% 이상 단축
- TPS 약 66배 향상

## 보조 개선 — 집계 결과 매핑 안정화

### Action
- 집계 결과 반환 타입을 `Map<String, Object>`에서 Projection 기반 조회로 전환했다.
- 서비스 계층의 문자열 키 파싱과 런타임 캐스팅을 제거했다.

### Result
- 집계 결과 매핑을 타입 안정적으로 정리하고, 서비스 파싱 로직을 단순화했다.

## 3차 개선 — 캐시 갱신 안정성 개선

### Problem
- 배치 완료 직후 캐시가 비워지면 첫 사용자 요청이 캐시 미스 비용을 직접 부담할 수 있었다.
- 배치 실패 시 캐시 갱신이 잘못되면 조회 안정성까지 흔들릴 수 있었다.

### Action
- 배치 성공 후 커밋 시점에만 캐시를 갱신하도록 순서를 재설계했다.
- 성공 후 warm-up을 수행하고, 실패 시에는 기존 캐시를 유지하도록 했다.

### Result Template
- 배치 직후 첫 요청: `cache miss → cache hit`
- 배치 직후 첫 요청 지연: `Xms → Yms`
- 실패 시 기존 캐시 유지: `검증 완료`

## Resume Bullet Candidates
- `반복 실시간 집계가 발생하던 대시보드 조회를 선계산 테이블 + Redis 캐시 구조로 전환해 응답 시간을 99% 이상 단축하고 TPS를 약 66배 향상`
- `집계 결과 매핑을 Map 기반에서 Projection 기반으로 전환해 문자열 키 의존과 런타임 캐스팅을 제거`
- `배치 성공 후 캐시 warm-up과 실패 시 기존 캐시 유지 흐름을 적용해 배치 직후 첫 요청을 cache miss에서 cache hit로 전환`

