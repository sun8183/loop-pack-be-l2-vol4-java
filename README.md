# Loopers Commerce Backend

Loopers 백엔드 부트캠프 10주 과정에서 진행한 이커머스 도메인 백엔드 프로젝트입니다.
상품/브랜드/좋아요 같은 기본 도메인 설계에서 시작해, 동시성 제어, 대규모 트래픽 대응(대기열),
이벤트 기반 데이터 정합성(실시간 랭킹)까지 단계적으로 확장하며 구현했습니다.

> 현재도 계속 리팩토링이 진행 중인 프로젝트입니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language / Framework | Java 21, Spring Boot 3.4 |
| Build | Gradle (Kotlin DSL), 멀티모듈 |
| Persistence | Spring Data JPA, QueryDSL, MySQL 8.0 |
| Cache | Redis (master-replica) |
| Messaging | Kafka (KRaft) |
| Batch | Spring Batch |
| Resilience | Resilience4j |
| Test | JUnit 5, Testcontainers, Instancio |

## 모듈 구조

```text
Root
├── apps ( 실행 가능한 Spring Boot 애플리케이션 )
│   ├── 📦 commerce-api        # REST API 서버
│   ├── 📦 commerce-batch      # 배치 처리 (랭킹 집계 등)
│   ├── 📦 commerce-streamer   # Kafka 이벤트 소비/반영 서버
│   └── 📦 pg-simulator        # 결제(PG) 연동 시뮬레이터
├── modules ( 재사용 가능한 인프라 설정 )
│   ├── 📦 jpa
│   ├── 📦 redis
│   └── 📦 kafka
└── supports ( 부가 기능 add-on )
    ├── 📦 jackson
    ├── 📦 monitoring
    ├── 📦 logging
    └── 📦 resilience
```

레이어드 아키텍처(`interfaces → application → domain → infrastructure`)와 DIP를 따르며,
Facade가 여러 도메인 Service를 조합해 유스케이스를 완성하는 구조를 일관되게 적용했습니다.

## 핵심 기능 & 기술적 의사결정

### 1. 동시성 제어
재고 차감, 쿠폰 발급 등 동시 요청이 몰리는 지점에 대해 동시성 테스트를 먼저 작성하고 문제를 재현한 뒤 보완했습니다.

**사례: 멀티 아이템 주문 데드락**
- **원인**: 여러 재고를 한 번에 주문할 때 `LinkedHashMap`으로 아이템을 병합해, 클라이언트가 보낸 요청 순서가 그대로 row lock 획득 순서가 됨 → 두 트랜잭션이 서로 반대 순서로 lock을 걸면 순환 대기 발생
- **재현**: 홀수 번째 요청은 `[stock1→stock2]`, 짝수 번째 요청은 `[stock2→stock1]` 순서로 동시 주문해 간헐적 500 (MySQL Deadlock) 확인
- **해결**: 병합 결과를 `stockId` 오름차순으로 정렬 후 lock 획득 → 모든 트랜잭션이 항상 같은 순서로 잠그므로 순환 대기 조건 자체가 성립하지 않음

**사례: 쿠폰 동시성**
- **선착순 발급**: 재고 100개 쿠폰에 200명이 동시에 비동기 발급 요청해도 정확히 100명만 성공하도록 검증
- **1회성 사용**: 같은 유저가 서로 다른 상품(재고 락이 겹치지 않는 조건)으로 동일 쿠폰을 동시에 5번 사용 시도해도 1건만 성공하도록 검증(쿠폰 자체의 동시성 제어가 재고 락에 기대지 않고 독립적으로 동작하는지 확인)

### 2. 주문 대기열
트래픽이 몰리는 주문 진입 구간에 대기열/토큰 검증 방식을 도입하고, 폴링 API로 입장 상태를 확인하도록 구현했습니다.
스케줄러 기반으로 입장 처리 속도(`batch-size`)를 조절해 몰림 현상을 완화했으며, 이 값은 감이 아니라 부하테스트 실측으로 산정했습니다.

**2단 방어: admission 페이스 제어 + 전역 rate limiter**

대기열 admission은 "입장 속도"만 제어할 뿐, `ALLOWED` 이후 5분 TTL 동안 유저가 언제 주문을 누르는지는 제어하지 못합니다. 그 시간 동안 쌓인 사람들이 비슷한 시점에 몰리면 admission 페이스와 무관하게 주문 API가 순간 과부하될 수 있어, Redis Lua 스크립트로 구현한 전역 토큰버킷(`capacity`/`refill=50`, 부하테스트로 확인한 안전 상한 그대로 사용)을 별도로 걸어 순간 몰림을 흡수합니다.

- `RATE=20~80` 단계별 고정 부하 테스트 결과, **RATE=50까지는 p95 지연 100~140ms대로 안정적**이다가 **RATE=60부터 p95가 13.9초로 급증**
- 실패율은 전 구간 0.5%대로 균일해, 처리량이 아니라 지연시간이 실제 캐패시티 경계선임을 확인 → **안전 상한 50 TPS로 확정**
- 안전 상한(50)에 10 TPS 마진을 두고 `batch-size=40`으로 운영값 결정

**한계 및 개선 방향**

전역 토큰버킷은 공정성을 보장하지 않습니다 — 거절된 요청은 대기열 순번을 잃고 새 요청으로 재시도하므로, 클라이언트가 고정 간격으로 재시도하면 특정 유저가 재시도 경쟁에서 계속 밀리는 이론적 starvation 가능성이 있습니다. 개선 방향은 (1) 거절 시 원 순번을 보존해 재편입, (2) 클라이언트 재시도에 exponential backoff + jitter 적용입니다.

**추가 검증: 처리량을 더 잘게 쪼개 몰림을 완화하면 어떨까?**

admission을 더 잘게 쪼갠 `batch=4/delay=100ms`(기존 `batch=40/delay=1000ms` 대비 10배 세분화)로 A/B 비교했습니다.

- 실유저 흐름(입장 → 폴링 → 주문)을 그대로 재현하는 테스트로 200명 동시 진입 3회 반복 측정
- 주문 처리 구간(`order_latency`)만 보면 세분화 쪽이 근소하게 빠름 — 그런데 **대기열 통과까지 걸리는 시간(`admission_wait`)이 45~50% 더 걸림**(tick을 10배 자주 도는 오버헤드 누적으로 추정)
- 유저 체감 전체 시간(입장~주문완료)으로 합치면 세분화 쪽이 **오히려 34% 더 느림** → 미채택, 기존 `batch=40/delay=1000ms` 유지
- **결론**: 이 시스템의 병목은 "순간 동시성 스파이크"가 아니라 "초당 처리량 자체가 50~60을 넘으면 뒷단이 못 버티는 구조적 캐패시티 한계"였음. 그래서 부하를 잘게 쪼개는 평탄화 전략은 총 처리량이 그대로면 효과가 없고, 오히려 오버헤드만 늘려 역효과였습니다.

**부하테스트 과정에서 발견/수정한 버그**
- `spring.task.scheduling.pool.size` 기본값(1)으로 인해 대기열 스케줄러와 아웃박스 릴레이가 스레드 하나를 공유하며 서로를 지연시키던 문제 → 2로 명시
- 대기열 검증 조회가 Redis replica를 우선 읽어, 복제 지연 시 방금 입장한 유저가 403(미입장)으로 오탐되던 문제 → 해당 조회를 master 전용으로 변경

### 3. 이벤트 기반 실시간 랭킹 시스템
좋아요/주문 도메인 이벤트를 Kafka로 발행하고(`commerce-api`), `commerce-streamer`가 이를 소비해 Redis/MySQL에 반영합니다.

- **정합성**: 주문 알림 이벤트(`OrderPlacedEvent`)는 Outbox 패턴으로 같은 트랜잭션 내 저장 후 별도 릴레이가 발행해 발행 유실까지 방지, 좋아요 이벤트는 `AFTER_COMMIT` 시점에 발행해 롤백된 트랜잭션의 팬텀 이벤트 발행만 방지(발행 자체의 유실 가능성은 남아있음)
- **멱등성**: Redis는 Lua 스크립트로 dedup 체크 + `ZINCRBY`를 원자적으로 실행, DB는 `event_key` UNIQUE 제약으로 중복 반영 방지
- **조회 최적화**: 오늘자 랭킹은 Redis ZSET에서 실시간 조회, 과거 랭킹은 배치로 집계된 MySQL 데이터를 1시간 캐싱해 조회
- **배치 집계**: 좋아요/주문 가중치(0.2 : 0.8)를 반영한 일별 랭킹을 새벽 스케줄러로 재계산

### 4. 조회 성능 최적화
Redis를 master(쓰기)/readonly(읽기)로 분리하고, 상품 조회 등 읽기 비중이 높은 API에 캐시를 적용했습니다.

- **비정규화**: 상품의 최저가(`minPrice`), 찜 수(`likeCount`)를 조인/집계 없이 바로 조회할 수 있도록 컬럼으로 저장 (재고/찜 변경 시 애플리케이션 레이어에서 동기화)
- **인덱스 설계**: 목록 조회가 `status/deletedAt` 고정 조건 + `brandId` 선택 조건 + 정렬(`createdAt`/`minPrice`/`likeCount`) 조합으로 이뤄지는 점을 고려해, brandId 유무 × 정렬 3종 조합으로 복합 인덱스 6개 구성

### 5. 결제 연동
PG 시뮬레이터와 연동하며 Resilience4j로 재시도/타임아웃을 제어해 외부 시스템 장애에 대비했습니다.

**서킷 브레이커: 결제 요청 vs 결제 조회 분리 설정**

결제 요청(`paymentGateway`)과 결제 상태 조회(`paymentGatewayQuery`)는 장애 시 리스크가 달라 서킷 브레이커 인스턴스를 분리했습니다. 요청은 중복 승인 위험이 있어 재시도를 걸지 않고, 조회는 멱등하므로 재시도를 추가했습니다.

| 설정 | `paymentGateway` (요청) | `paymentGatewayQuery` (조회) |
|------|------|------|
| failure-rate-threshold | 50% | 50% |
| minimum-number-of-calls | 5 | 6 |
| wait-duration-in-open-state | 30s | 10s |
| permitted-calls-in-half-open | 3 | 2 |
| sliding-window | COUNT_BASED, 10 | COUNT_BASED, 15 |
| Retry | 없음 | max-attempts 3, wait 500ms, `ResourceAccessException`에만 |

- 두 인스턴스 모두 `HttpClientErrorException`(4xx), `CoreException`(도메인 예외)은 서킷 카운트에서 제외 — 클라이언트 잘못까지 서킷을 열리게 하지 않기 위함
- 조회는 `wait-duration-in-open-state`를 짧게(10s) 잡아 반정상 상태로 빨리 복귀 시도 — 조회 재시도는 상태를 바꾸지 않아 부담이 적음
- 설정 위치: `apps/commerce-api/src/main/resources/application.yml`, 적용부: `RestTemplatePaymentGatewayClient`

## 로컬 환경 실행

### 인프라 (MySQL, Redis, Kafka)
```shell
docker-compose -f ./docker/infra-compose.yml up
```

### 애플리케이션 실행
```shell
./gradlew :apps:commerce-api:bootRun
./gradlew :apps:commerce-batch:bootRun
./gradlew :apps:commerce-streamer:bootRun
```
