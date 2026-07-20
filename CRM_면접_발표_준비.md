# CRM 프로젝트 3분 발표 및 기술 면접 대비

## 0. 발표에서 반드시 지킬 기준

- 성능 수치는 `backend-spring/docs/dashboard-performance/04-portfolio-dashboard-writeup.md`에 저장된 측정 결과 기준이라고 밝힌다.
- 개선 후 개별 응답 시간이나 테스트 데이터 규모는 저장된 문서에서 확인되지 않으므로 임의로 말하지 않는다.
- Access Token 유효시간은 코드 계산식 기준 **1시간**, Refresh Token은 **7일**이다.
- Redis 장애 시 자동으로 DB 조회로 전환되는 구조는 아니다. 캐시 미스에는 DB를 조회하지만 Redis 서버 장애는 별도 예외 대응이 필요하다.

---

## 1. 3분 프로젝트 발표 대본

안녕하십니까. 이어서 제가 직접 백엔드 개발에 참여한 OJO CRM 프로젝트를 설명드리겠습니다.

이 프로젝트는 통신 고객의 가입, 결제, 상담, 서비스 이용 데이터를 통합해 고객 현황과 이탈 위험, RFM 분석 결과를 제공하는 CRM 서비스입니다. 전체 시스템은 React 프론트엔드, Spring Boot API와 배치 서버, Python 분석 서버로 구성했습니다. 저는 Spring 백엔드에서 대시보드 성능 개선과 JWT 인증 구조를 직접 구현했습니다.

첫 번째로 해결한 문제는 대시보드 조회 성능입니다. 초기 구조에서는 사용자가 대시보드를 조회할 때마다 여러 테이블을 대상으로 고객 수와 세그먼트 통계를 실시간 집계했습니다. 데이터와 동시 요청이 증가하면서 저부하에서도 평균 응답 시간이 약 6초였고, 고부하에서는 평균 약 19.7초, p95 약 35.1초까지 증가했습니다.

원인을 분석한 결과, 자주 조회되지만 하루 동안 급격하게 바뀌지 않는 통계를 매 요청마다 다시 계산하는 구조가 병목이었습니다. 그래서 조회 시점의 연산을 줄이기 위해 배치 과정에서 통계를 미리 계산해 요약·일별 통계 테이블에 저장하고, 조회 결과를 Redis에 5분 동안 캐싱하도록 변경했습니다. 조회 API는 원본 테이블을 다시 집계하지 않고 선계산 테이블을 읽으며, 캐시가 있으면 Redis 결과를 바로 반환합니다.

여기서 단순히 캐시를 비우는 데 그치지 않고 데이터 정합성도 고려했습니다. 통계 저장 트랜잭션이 성공적으로 커밋된 이후에만 기존 캐시를 삭제하고, 즉시 조회 메서드를 호출해 새 데이터로 캐시를 warm-up했습니다. 따라서 집계 도중 실패하면 기존 캐시를 유지하고, 성공 직후 첫 사용자에게 캐시 미스 비용이 집중되는 것도 방지했습니다. 저장된 성능 측정 결과 기준으로 응답 시간을 99% 이상 단축하고 TPS를 약 66배 높였습니다.

두 번째는 JWT 인증 구조입니다. Access Token은 1시간, Refresh Token은 7일로 역할과 수명을 분리했습니다. Refresh Token은 DB에 사용자별로 한 개만 저장하고, 재발급할 때 전달받은 토큰과 DB 토큰이 일치하는지 확인한 뒤 Access Token과 Refresh Token을 모두 새로 발급하는 Rotation 방식을 적용했습니다. 불일치하면 저장된 토큰을 삭제하고, 로그아웃 시에도 Refresh Token을 삭제해 이후 재발급을 차단했습니다.

또한 JWT가 아직 유효하더라도 관리자가 비활성화된 계정이면 요청 필터에서 DB 상태를 확인해 인증 객체를 생성하지 않도록 했습니다. 이를 통해 토큰 만료를 기다리지 않고 계정 상태 변경을 즉시 반영할 수 있었습니다.

이 경험을 통해 기능을 추가하는 것보다 요청마다 어떤 연산과 검증이 반복되는지 먼저 추적하는 것이 중요하다는 점을 배웠습니다. 이후에는 문제를 바로 수정하기보다 전체 요청 흐름과 데이터 갱신 시점을 분석하고, 성능과 정합성을 함께 검토하는 방식으로 개발하고 있습니다. 감사합니다.

### 발표 시간 조절용 문장

- 20초 초과 시 삭제: 프로젝트의 RFM·이탈 위험 설명과 시스템 구성 세부 문장
- 20초 부족 시 추가: “대시보드 집계는 매일 실행되는 배치 파이프라인의 마지막 단계에 연결해 분석 결과가 모두 확정된 뒤 갱신되도록 했습니다.”

---

## 2. 발표 구조 암기용 키워드

1. **개요**: 통신 고객 CRM → React / Spring / Python → 담당: 성능·인증
2. **문제**: 요청마다 실시간 집계 → 6초 / 고부하 19.7초 / p95 35.1초
3. **분석**: 자주 읽고 상대적으로 천천히 변하는 통계를 매번 재계산
4. **행동**: 배치 선계산 → 통계 테이블 → Redis 5분 캐시
5. **정합성**: 커밋 성공 후 clear → warm-up → 실패 시 기존 캐시 유지
6. **결과**: 저장된 측정 기준 99% 이상 단축 / TPS 약 66배
7. **인증**: Access 1시간 / Refresh 7일 → DB 비교 → Rotation → 로그아웃 삭제
8. **통제**: 요청마다 계정 ACTIVE 확인 → 비활성 계정 즉시 차단
9. **마무리**: 코드 수정 전에 요청 흐름과 데이터 갱신 구조 분석

---

## 3. 기술 면접 Q&A

### Q1. 기존 대시보드는 왜 느렸습니까?

**짧은 답변**  
조회 요청마다 여러 원본 테이블의 고객·세그먼트 통계를 반복 집계했기 때문입니다. 데이터와 동시 요청이 늘수록 같은 고비용 연산이 중복 실행됐습니다.

**코드 기반 상세 답변**  
개선된 `DashboardAggregationService.refreshDashboardStats()`는 현재 고객, 신규 고객, 위험 고객, 세그먼트와 최근 7일 통계를 한 번 계산해 `DashboardSummaryStats`와 `DashboardDailyStats`에 저장합니다. 이후 `DashboardService.getDashboardSummary()`는 원본 데이터 대신 이 통계 테이블 두 곳만 조회합니다. 즉, 연산량이 큰 쓰기 시점의 집계와 읽기 시점의 단순 조회를 분리했습니다.

**추가 꼬리질문**

- 어떤 쿼리가 가장 느렸는가?
- 인덱스만 추가하는 방법과 비교했는가?
- 데이터가 실시간으로 바뀌면 통계는 언제 반영되는가?

**답변 포인트**  
저장된 자료에는 개별 쿼리별 실행 시간은 없으므로 특정 쿼리가 가장 느렸다고 단정하지 않는다. 인덱스 SQL은 존재하지만, 핵심 개선은 반복 집계 자체를 요청 경로에서 제거한 것이다. 현재 통계 갱신은 일일 배치 마지막 단계에서 수행된다.

**코드 근거**: `DashboardAggregationService.refreshDashboardStats()`, `DashboardService.getDashboardSummary()`

### Q2. 왜 실시간 집계 대신 선계산을 선택했습니까?

**짧은 답변**  
대시보드 통계는 조회 빈도는 높지만 매 요청 순간의 완전한 실시간성이 필수인 데이터는 아니었기 때문에, 계산 비용을 배치 시점으로 이동하는 것이 적합했습니다.

**코드 기반 상세 답변**  
`MemberFeatureScheduler`는 고객 특성 계산, RFM 전처리, Python 분석, KPI 후처리가 모두 성공한 뒤 마지막 단계에서 대시보드 집계를 실행합니다. 앞 단계가 실패하면 이후 단계를 실행하지 않으므로 불완전한 분석 결과로 대시보드를 갱신하지 않습니다.

**추가 꼬리질문**

- 실시간성이 꼭 필요한 지표라면 어떻게 하겠는가?
- 배치가 실패하면 사용자는 오래된 값을 보게 되지 않는가?

**답변 포인트**  
실시간 지표라면 이벤트 기반 증분 집계나 짧은 주기의 갱신을 검토한다. 현재 구조는 최신성보다 안정적인 조회 성능을 선택했으며, 실패 시 잘못된 새 값보다 마지막 정상 값을 유지한다.

**코드 근거**: `MemberFeatureScheduler.runMemberFeatureJob()`

### Q3. Cache Aside 패턴은 어떻게 동작합니까?

**짧은 답변**  
`@Cacheable`이 먼저 Redis의 `dashboardCache::summary`를 확인하고, 값이 없을 때만 서비스 로직이 통계 테이블을 조회한 뒤 결과를 캐시에 저장합니다.

**코드 기반 상세 답변**  
`DashboardService.getDashboardSummary()`에 `@Cacheable(value = "dashboardCache", key = "'summary'")`가 적용돼 있습니다. 캐시 hit이면 메서드 본문을 실행하지 않고 저장된 DTO를 반환하고, miss이면 통계 테이블을 조회해 응답을 만든 뒤 Redis에 저장합니다. TTL은 `RedisCacheConfig`에서 5분으로 설정했습니다.

**추가 꼬리질문**

- TTL을 왜 5분으로 정했는가?
- Redis가 죽으면 DB로 자동 fallback 되는가?

**답변 포인트**  
5분은 현재 설정값이지만, 트래픽과 허용 가능한 최신성에 근거해 튜닝해야 한다. 캐시 miss와 Redis 서버 장애는 다르다. 현재 별도 `CacheErrorHandler`가 없어 Redis 장애 시 자동 DB fallback을 보장하지 않는다.

**코드 근거**: `DashboardService.getDashboardSummary()`, `RedisCacheConfig.cacheManager()`

### Q4. 왜 트랜잭션 커밋 이후에 캐시를 갱신합니까?

**짧은 답변**  
DB 저장이 롤백됐는데 캐시만 새로운 상태로 바뀌는 정합성 문제를 막기 위해서입니다.

**코드 기반 상세 답변**  
`refreshDashboardStats()`는 트랜잭션 안에서 통계 데이터를 저장하고 `TransactionSynchronizationManager`에 콜백을 등록합니다. `afterCommit()`이 호출된 뒤에만 캐시를 clear하고 새 통계를 조회해 warm-up합니다. 커밋 전에 예외가 발생하면 콜백이 실행되지 않으므로 기존 캐시가 유지됩니다. 단위 테스트도 커밋 전에는 clear와 warm-up이 호출되지 않고, `afterCommit()` 후에만 호출되는지 검증합니다.

**추가 꼬리질문**

- 캐시 삭제 후 warm-up 전에 장애가 나면 어떻게 되는가?
- DB 커밋과 Redis 갱신을 하나의 트랜잭션으로 묶을 수 있는가?

**답변 포인트**  
clear 이후 warm-up 실패 시 다음 조회가 DB에서 값을 읽어 캐시를 채우지만, Redis 장애 자체는 별도 대응이 필요하다. DB와 Redis의 원자적 커밋을 억지로 묶기보다 재시도, 모니터링, 이벤트/Outbox 방식 등을 고려할 수 있다.

**코드 근거**: `DashboardAggregationService.scheduleDashboardCacheRefreshAfterCommit()`, `DashboardAggregationServiceTest.refreshesDashboardCacheOnlyAfterSuccessfulCommit()`

### Q5. warm-up을 왜 했습니까?

**짧은 답변**  
배치 직후 첫 사용자가 캐시 miss와 DB 조회 비용을 모두 부담하는 cold-start를 방지하기 위해서입니다.

**코드 기반 상세 답변**  
커밋 후 캐시를 비운 다음 `dashboardService.getDashboardSummary()`를 즉시 호출합니다. 별도 Bean인 `DashboardService`를 호출하므로 Spring Cache 프록시가 동작해 새 응답이 Redis에 저장됩니다. 통합 측정 테스트는 두 번째 갱신 직후에도 `summary` 키가 존재하는지 확인합니다.

**추가 꼬리질문**

- 같은 클래스 내부에서 캐시 메서드를 호출하면 어떻게 되는가?

**답변 포인트**  
자기 호출은 프록시를 통과하지 않아 `@Cacheable`이 적용되지 않을 수 있다. 현재는 별도 서비스 Bean을 주입받아 호출한다.

**코드 근거**: `DashboardAggregationService`, `DashboardCacheRefreshMeasurementIT.measureCacheStateAroundDashboardRefresh()`

### Q6. 99% 단축과 TPS 66배는 어떻게 설명해야 합니까?

**짧은 답변**  
프로젝트에 저장된 부하 측정 결과 문서 기준 수치이며, 기존 고비용 집계를 요청 경로에서 제거한 전후 결과입니다.

**코드 기반 상세 답변**  
저장된 baseline은 저부하 평균 약 6,078ms·처리량 약 1.4/sec, 고부하 평균 약 19,740ms·p95 약 35,101ms·처리량 약 1.2/sec·오류율 0%입니다. 결과 문서에는 개선 후 응답 시간 99% 이상 단축과 TPS 약 66배 향상이 기록돼 있습니다.

**추가 꼬리질문**

- 테스트 데이터는 몇 건이었는가?
- 개선 후 정확한 평균과 p95는 얼마였는가?
- 동일한 환경에서 측정했는가?

**답변 포인트**  
현재 저장된 결과만으로 데이터 건수와 개선 후 개별 지표를 확인할 수 없다면 모른다고 명확히 답한다. “문서에 남아 있는 동일 시나리오 측정 결과 기준”이라고 범위를 제한하고, 재현 시에는 데이터 건수·동시 사용자·warm/cold cache·반복 횟수를 함께 기록하겠다고 답한다.

**코드 근거**: `docs/dashboard-performance/04-portfolio-dashboard-writeup.md`, `DashboardCacheRefreshMeasurementIT`

### Q7. 평균, p95, TPS는 각각 무엇을 의미합니까?

**짧은 답변**  
평균은 전체 요청의 평균 지연, p95는 95%의 요청이 그 시간 안에 끝난다는 의미이며, TPS는 초당 처리한 요청 수입니다.

**코드 기반 상세 답변**  
평균만 보면 일부 매우 느린 요청이 가려질 수 있어 꼬리 지연을 나타내는 p95를 함께 봐야 합니다. 고부하 baseline에서 평균 19.7초와 p95 35.1초가 모두 높았으므로 일부 요청만의 문제가 아니라 전반적인 지연과 긴 꼬리 지연이 함께 존재했다고 설명할 수 있습니다.

**추가 꼬리질문**

- TPS가 높으면 무조건 좋은가?
- p99도 봐야 하지 않는가?

**답변 포인트**  
오류율과 응답 시간을 함께 만족할 때 의미가 있다. 운영 중요도와 트래픽 규모에 따라 p99도 함께 측정해야 한다.

### Q8. Access Token과 Refresh Token을 왜 분리했습니까?

**짧은 답변**  
짧은 Access Token으로 API를 인증하면서, 긴 로그인 유지와 서버 측 재발급 통제를 Refresh Token으로 분리하기 위해서입니다.

**코드 기반 상세 답변**  
Access Token에는 사용자 ID, 이메일, 역할, `typ=access`가 있고 유효시간은 1시간입니다. Refresh Token에는 사용자 ID와 `typ=refresh`만 넣고 7일로 설정했습니다. API 필터는 Access Token만 인증에 사용하며, 재발급 API는 Refresh Token의 서명·만료·타입과 DB 저장값을 모두 확인합니다.

**추가 꼬리질문**

- Refresh Token에 역할을 넣지 않은 이유는?
- Access Token을 즉시 무효화할 수 있는가?

**답변 포인트**  
재발급 시 DB에서 최신 사용자 상태와 역할을 다시 읽으므로 Refresh Token에 권한을 고정하지 않는다. 현재 Access Token 자체를 blacklist 처리하지는 않지만, 필터가 매 요청 관리자 상태를 조회해 비활성 계정은 즉시 차단한다.

**코드 근거**: `JwtProvider.generateAccessToken()`, `generateRefreshToken()`, `JwtAuthenticationFilter.doFilterInternal()`

### Q9. Refresh Token Rotation 흐름을 설명해 주세요.

**짧은 답변**  
재발급에 성공할 때마다 Access Token뿐 아니라 Refresh Token도 새로 발급하고, DB의 기존 토큰을 새 값으로 교체합니다.

**코드 기반 상세 답변**  
`AuthService.refresh()`는 요청 토큰의 유효성과 `typ=refresh`를 확인하고 subject에서 관리자 ID를 가져옵니다. DB에서 해당 사용자의 저장 토큰을 읽어 문자열이 같은지 비교합니다. 일치하면 사용자 상태를 확인하고 두 토큰을 새로 만든 뒤 `upsertRefreshToken()`으로 DB 값을 교체합니다. 이전 Refresh Token은 이후 DB 비교를 통과하지 못합니다.

**추가 꼬리질문**

- 탈취된 이전 토큰이 재사용되면 어떻게 되는가?
- 정상 사용자와 공격자가 동시에 재발급하면 어떻게 되는가?

**답변 포인트**  
이전 토큰이 새 토큰 발급 후 사용되면 mismatch로 저장 토큰까지 삭제해 추가 재발급을 차단한다. 다만 동시 재발급에는 비관적 락, 버전 컬럼, 원자적 조건부 업데이트가 없어 경쟁 조건이 생길 수 있다.

**코드 근거**: `AuthService.refresh()`, `AuthService.upsertRefreshToken()`

### Q10. 로그아웃은 어떻게 처리됩니까?

**짧은 답변**  
인증된 사용자 ID로 DB의 Refresh Token을 삭제해 이후 재발급을 차단합니다.

**코드 기반 상세 답변**  
`AuthController.logout()`은 SecurityContext의 `AdminPrincipal`에서 관리자 ID를 얻고 `AuthService.logout()`을 호출합니다. 서비스는 `deleteByAdminId()`로 저장된 Refresh Token을 삭제합니다. 이미 발급된 Access Token은 남아 있지만 최대 1시간 후 만료되며, 비활성 계정 여부는 요청마다 별도로 검사합니다.

**추가 꼬리질문**

- 로그아웃 즉시 Access Token도 막으려면?

**답변 포인트**  
Access Token의 남은 만료시간만큼 Redis blacklist에 토큰 식별자를 저장하거나 토큰 버전 방식을 사용할 수 있다. 그만큼 매 요청 저장소 조회와 상태 관리 비용이 추가된다.

**코드 근거**: `AuthController.logout()`, `AuthService.logout()`

### Q11. JWT인데 왜 매 요청 DB를 조회합니까?

**짧은 답변**  
완전한 무상태성보다 비활성 관리자 계정을 즉시 차단하는 운영 통제를 선택했기 때문입니다.

**코드 기반 상세 답변**  
`JwtAuthenticationFilter`는 서명과 토큰 타입을 검증한 뒤 subject의 관리자 ID로 DB를 조회합니다. 관리자가 존재하고 상태가 `ACTIVE`일 때만 `AdminPrincipal`과 인증 객체를 만들어 `SecurityContext`에 저장합니다.

**추가 꼬리질문**

- JWT의 장점이 사라지는 것 아닌가?
- DB 조회 병목은 어떻게 줄일 수 있는가?

**답변 포인트**  
세션 서버 상태는 없지만 매 요청 DB 의존성이 생기는 절충이다. 상태를 짧게 캐싱하거나 계정 버전·블랙리스트 전략을 사용할 수 있으나, 차단 반영 지연과 캐시 정합성을 함께 고려해야 한다.

**코드 근거**: `JwtAuthenticationFilter.doFilterInternal()`

### Q12. Spring Security 필터에서 인증 객체는 어떻게 만들어집니까?

**짧은 답변**  
JWT 필터가 토큰과 사용자 상태를 검증한 뒤 `UsernamePasswordAuthenticationToken`을 생성해 `SecurityContextHolder`에 저장합니다.

**코드 기반 상세 답변**  
필터는 Authorization 헤더의 `Bearer ` 접두사를 제거하고 Access Token만 허용합니다. claims에서 ID·이메일·역할을 읽어 `AdminPrincipal`을 만들고, principal의 authority를 인증 객체에 넣습니다. 필터는 `UsernamePasswordAuthenticationFilter`보다 앞에 등록돼 이후 인가 단계에서 해당 인증 정보를 사용할 수 있습니다.

**추가 꼬리질문**

- Refresh Token을 Authorization 헤더에 넣으면 어떻게 되는가?
- 필터가 예외를 던지지 않고 그냥 통과하는 이유는?

**답변 포인트**  
`isAccessToken()`을 통과하지 못해 인증 객체가 생성되지 않는다. 보호 API라면 이후 인가 단계에서 401이 반환된다.

**코드 근거**: `JwtAuthenticationFilter`, `SecurityConfig.filterChain()`

### Q13. 401과 403의 차이는 무엇입니까?

**짧은 답변**  
401은 인증되지 않은 요청이고, 403은 인증은 됐지만 해당 자원에 필요한 권한이 없는 요청입니다.

**코드 기반 상세 답변**  
`SecurityConfig`는 인증 실패와 접근 거부 처리를 `SecurityExceptionHandler`에 연결합니다. 일반 API는 인증을 요구하고, 관리자 관리 API는 `@PreAuthorize("hasRole('ADMIN')")`로 역할을 추가 검증합니다. 토큰이 없거나 유효하지 않으면 401, 인증된 일반 역할이 관리자 전용 API를 호출하면 403에 해당합니다.

**추가 꼬리질문**

- JWT의 role claim을 그대로 믿어도 되는가?

**답변 포인트**  
서명이 유효하므로 외부 변조는 검출하지만, 토큰 발급 후 역할 변경은 Access Token 만료 전까지 claim에 반영되지 않는다. 현재 필터의 DB 조회는 상태만 확인하고 최신 role은 다시 읽지 않는다는 한계가 있다.

**코드 근거**: `SecurityConfig`, `AdminController`, `SecurityExceptionHandler`

### Q14. Refresh Token을 DB에 평문 저장해도 됩니까?

**짧은 답변**  
현재 구현은 평문 저장이므로 DB가 노출되면 토큰 재사용 위험이 있습니다. 운영 수준에서는 해시 저장이 더 안전합니다.

**코드 기반 상세 답변**  
현재 `RefreshToken.token` 컬럼에 JWT 문자열을 그대로 저장하고 요청 토큰과 문자열 비교를 합니다. 개선한다면 Refresh Token에 고유 ID를 넣고 서버에는 SHA-256 등의 단방향 해시만 저장해 비교할 수 있습니다. 비밀번호처럼 느린 해시가 반드시 필요한 것은 아니지만 원문 복구가 불가능해야 합니다.

**추가 꼬리질문**

- Redis 저장과 DB 저장 중 무엇이 더 좋은가?

**답변 포인트**  
Redis는 TTL과 빠른 조회에 유리하고 DB는 영속성과 감사에 유리하다. 저장소 선택과 별개로 토큰 원문 노출 방지, 만료 정리, 장애 전략이 필요하다.

**코드 근거**: `RefreshToken`, `AuthService.upsertRefreshToken()`

### Q15. 현재 보안 설정에서 개선할 부분은 무엇입니까?

**짧은 답변**  
배치와 회원 메모 API가 전체 허용돼 있고, Refresh Token 평문 저장과 동시 재발급 제어도 보완해야 합니다.

**코드 기반 상세 답변**  
`SecurityConfig`는 `/batch/**`와 `/api/member-memos/**`를 `permitAll()`로 설정합니다. 실제 운영에서는 배치 API를 관리자 권한 또는 내부 네트워크로 제한하고, 메모 API도 업무 역할에 따라 보호해야 합니다. Refresh Token에는 해시 저장과 조건부 갱신을 적용하고, 비밀키와 유효시간은 환경 설정으로 분리하는 것이 좋습니다.

**추가 꼬리질문**

- 가장 먼저 고칠 항목은 무엇인가?

**답변 포인트**  
외부에서 접근 가능한 민감 API의 인증·인가 누락을 가장 먼저 수정한다. 이는 성능 개선보다 직접적인 보안 경계 문제다.

**코드 근거**: `SecurityConfig.filterChain()`, `AuthService.refresh()`, `JwtProvider`

---

## 4. 구현 한계와 개선 방향 요약

| 현재 구현 | 확인된 한계 | 개선 방향 |
|---|---|---|
| Redis `@Cacheable`, TTL 5분 | Redis 서버 장애 시 자동 DB fallback 없음 | `CacheErrorHandler`, timeout·circuit breaker, 장애 모니터링 |
| 커밋 후 clear + warm-up | clear 성공 후 warm-up 실패 가능 | 재시도 및 알림, 캐시 키 버전 교체 방식 |
| 일일 배치 선계산 | 배치 실패 시 최신 통계 반영 지연 | 실패 알림·재실행 정책, 지표별 증분 갱신 검토 |
| Refresh Token Rotation | 동시 재발급 경쟁 조건 | 비관적 락 또는 토큰 버전 기반 조건부 업데이트 |
| Refresh Token 평문 DB 저장 | DB 노출 시 토큰 재사용 가능 | 토큰 해시 저장, 만료 데이터 정리 |
| 요청마다 관리자 DB 조회 | JWT의 무상태·저지연 이점 감소 | 짧은 상태 캐시 또는 토큰 버전 전략 |
| 토큰 role claim 사용 | 발급 후 권한 변경 즉시 반영 안 됨 | 최신 역할 조회, 짧은 Access TTL, 권한 버전 적용 |
| 일부 API `permitAll()` | 민감 기능의 인증·인가 경계 약함 | 관리자 권한 및 내부 호출 인증 적용 |

---

## 5. 코드 근거 색인

### 대시보드 성능·정합성

- `backend-spring/src/main/java/org/backend/domain/analysis/service/DashboardAggregationService.java`
  - `refreshDashboardStats()`: 통계 선계산·요약/일별 테이블 저장
  - `scheduleDashboardCacheRefreshAfterCommit()`: 커밋 후 캐시 삭제와 warm-up
- `backend-spring/src/main/java/org/backend/domain/analysis/service/DashboardService.java`
  - `getDashboardSummary()`: 통계 테이블 조회와 `@Cacheable`
- `backend-spring/src/main/java/org/backend/config/cache/RedisCacheConfig.java`
  - `cacheManager()`: Redis 직렬화와 TTL 5분
- `backend-spring/src/main/java/org/backend/domain/batch/scheduler/MemberFeatureScheduler.java`
  - `runMemberFeatureJob()`: 단계별 성공 확인과 최종 대시보드 집계
- `backend-spring/src/test/java/org/backend/domain/analysis/service/DashboardAggregationServiceTest.java`
  - 커밋 성공 이후에만 캐시 갱신하는지 검증
- `backend-spring/src/test/java/org/backend/domain/analysis/service/DashboardCacheRefreshMeasurementIT.java`
  - 집계 직후 cache warm 상태 측정
- `backend-spring/docs/dashboard-performance/04-portfolio-dashboard-writeup.md`
  - baseline과 개선 결과 수치

### JWT 인증·인가

- `backend-spring/src/main/java/org/backend/config/security/JwtProvider.java`
  - Access/Refresh Token claim·유효시간·타입 검증
- `backend-spring/src/main/java/org/backend/domain/auth/service/AuthService.java`
  - 로그인, 재발급, Rotation, mismatch 폐기, 로그아웃
- `backend-spring/src/main/java/org/backend/config/security/filter/JwtAuthenticationFilter.java`
  - Bearer Token 검증, 관리자 상태 조회, SecurityContext 생성
- `backend-spring/src/main/java/org/backend/config/security/SecurityConfig.java`
  - Stateless 정책, 공개/보호 경로, 필터 순서, 예외 처리
- `backend-spring/src/main/java/org/backend/domain/auth/entity/RefreshToken.java`
  - 사용자별 Refresh Token 저장 구조
- `backend-spring/src/main/java/org/backend/domain/auth/controller/AuthController.java`
  - 재발급·로그아웃 API 흐름

---

## 6. 마지막 점검 체크리스트

- [ ] 발표를 읽지 않고 `문제 → 원인 → 선택 → 구현 → 결과` 순서로 말할 수 있다.
- [ ] 6,078ms, 19,740ms, p95 35,101ms, 99%, 66배의 의미와 근거 범위를 설명할 수 있다.
- [ ] 캐시 miss와 Redis 장애를 구분해서 설명할 수 있다.
- [ ] `afterCommit`을 커밋 전 캐시 갱신과 비교해 설명할 수 있다.
- [ ] Access 1시간, Refresh 7일과 Rotation 흐름을 화이트보드에 그릴 수 있다.
- [ ] 현재 구현의 보안·동시성 한계를 질문받아도 방어하지 않고 개선안까지 답할 수 있다.
- [ ] 모르는 측정 조건이나 팀 성과를 추측하지 않고 확인 가능한 범위만 말한다.
