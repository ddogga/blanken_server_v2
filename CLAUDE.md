# CLAUDE.md — blanken 서버

영어 빈칸채우기 퀴즈 학습 앱의 **백엔드 API 서버**. 클라이언트는 여러 종류를 전제한다(§1 참고).

- **원본 기획서**: `/Users/user/Documents/code/blanken_document/PRD_퀴즈학습앱_v2.md` (v0.2, 2026-08-20)
  이 문서는 그 PRD에서 **서버 구현에 영향을 주는 결정만** 추린 요약본이다. 세부 배경·근거가 필요하면 PRD를 직접 읽을 것.
- 사용자와의 대화는 한국어로 진행한다.

---

## 0. 작업 방식 (가장 먼저 지킬 것)

**코드를 수정하기 전에 반드시 사용자에게 먼저 묻는다.** 승인 없이 파일을 편집하지 않는다.

- **질문에는 답만 한다.** "어떻게 해야 해?", "이게 맞아?", "이거 동작할까?", "어떻게 하는 게 좋을까?" 는 **설명을 요청한 것이지 코드를 고쳐달라는 뜻이 아니다.**
  답을 하면서 필요하면 코드 예시를 **메시지 안에** 보여주고, 파일에 반영할지는 사용자가 정한다.
- **편집해도 되는 경우는 사용자가 명시적으로 요청했을 때뿐이다.** "구현해줘", "수정해줘", "정리해줘", "추가해줘" 처럼 행위를 지시한 경우.
  그 경우에도 **요청한 범위를 벗어나는 파일은 건드리지 않는다.** 범위 밖에 손댈 필요가 생기면 먼저 말하고 승인을 받는다.
- **읽기·조사·빌드·테스트 실행은 물어보지 않아도 된다.** 파일을 바꾸지 않는 행위는 자유롭게 한다.
  단, 앱을 띄우거나 DB에 데이터를 넣고 지우는 것처럼 **상태를 바꾸는 검증**은 미리 알린다.
- 고칠 곳을 발견하면 **고치지 말고 보고한다.** 어디가 왜 문제인지, 어떻게 고치면 되는지까지만 제시한다.

이 프로젝트는 학습과 포트폴리오를 겸하므로, **코드를 직접 쓰는 주체는 사용자**다.
에이전트가 앞서 나가 고쳐 버리면 사용자가 판단할 기회와 맥락을 잃는다.

---

## 1. 제품 개요

사용자가 직접 만든 영어 문장으로 빈칸 채우기 퀴즈셋을 만들어 공유하고, 다른 유저와 실시간 선착순 대결까지 하는 학습 앱.
토이 프로젝트가 아니라 **실제 배포·운영되는 C2C 서비스**를 지향한다.

**핵심 원칙**: 목표 스택(Redis / 메시지큐 / 동시성 제어 / K8s)을 **도메인이 실제로 요구하는 지점에만** 적용한다.
"기술을 억지로 끼워 넣지 않고, 필요 없는 곳에는 쓰지 않는 판단"까지 설계의 일부다. 새 기술 도입을 제안할 때는 도메인 근거를 함께 제시할 것.

**클라이언트**: **멀티 클라이언트를 전제한다.** 첫 타깃은 안드로이드 네이티브(Kotlin)이고, 이후 React(웹)·Flutter 클라이언트를 추가할 계획이다.
모두 클라이언트 렌더링이며 서버는 JSON API만 제공한다 — 빌드·배포 파이프라인이 서버와 분리된다.

이 전제에서 따라오는 것 (2026-09-29 결정)
- **서버가 화면을 그리거나 리다이렉트로 흐름을 제어하지 않는다.** 인증도 예외가 아니다. 브라우저 리다이렉트와 세션 쿠키에 기대는 방식(서버 주도 OAuth Authorization Code)은 네이티브 앱에서 성립하지 않으므로, **클라이언트 종류와 무관하게 동작하는 토큰 기반 인증**을 기본으로 둔다. 단 웹 클라이언트가 실제로 들어오면 서버 주도 플로우도 선택지에 남는다 — 어느 쪽이든 **한 가지로 통일**하고 클라이언트별로 갈라지지 않게 한다.
- **§4.1의 클라이언트 채점이 클라이언트 수만큼 복제된다.** 정답 정규화 규칙(대소문자·공백·유니코드)의 **단일 출처** 유지가 클라이언트가 하나일 때보다 훨씬 중요해진다. 규칙은 문서·상수로 못 박고, 구현이 갈라지면 같은 답이 클라이언트마다 다르게 채점된다.
- **CORS 설정이 필요해진다.** 네이티브만 있을 때는 불필요하지만 웹 클라이언트가 붙는 순간 필요하다. → `SecurityConfig.corsConfigurationSource` 에 구현됨 (`app.cors.allowed-origins`).

> **현재 구현과 위 원칙의 간극** — 실제 인증은 Spring Security `oauth2Login`(서버 주도 Authorization Code)으로 구현되어 있고, 로그인 성공 시 **프론트 URL(`app.oauth2.redirect.*`)로 리다이렉트**한다(§2.2). 결과물은 JWT라 이후 API 호출은 클라이언트 종류와 무관하지만, **로그인 진입 플로우는 브라우저 리다이렉트에 기대고 있다.** 네이티브 클라이언트 대응 방식은 §9 미결정 사항.

**전제**: 로그인 없이는 앱 진입이 불가능하다. 모든 API는 인증된 사용자를 전제한다. (단 현재 `SecurityConfig` 는 개발 편의상 `anyRequest permitAll` — §4.10 부채 참고)

---

## 2. 현재 저장소 상태

**M1 거의 완료 + M2 일부 착수** (2026-10-12 기준).
인증(소셜 로그인 + JWT), 회원가입·탈퇴, 퀴즈셋/퀴즈 CRUD·검색, 학습 기록 생성, 좋아요, 팔로우까지 구현됨.
다음 단계는 **Docker 빌드 + Flyway 마이그레이션 + AWS CI/CD 인프라 구축**이다 (§9).

### 2.1 스택

| 항목 | 값 |
|---|---|
| 언어/런타임 | Kotlin 2.3.21, Java 21 toolchain |
| 프레임워크 | Spring Boot 4.1.0 (`spring-boot-starter-webmvc`, `-data-jpa`) |
| DB | PostgreSQL (`ddl-auto: ${DDL_AUTO:update}`, `open-in-view: false`). 검색용 `pg_trgm` 확장 + GIN 인덱스는 `schema.sql` 로 **수동 적용** |
| 인증 | `spring-boot-starter-security` + `-oauth2-client` (카카오·네이버), JWT는 jjwt 0.12.6 |
| Redis | `spring-boot-starter-data-redis` — 현재 용도는 **Refresh Token 저장**과 **OAuth2 인가 요청 임시 보관**뿐 (§7 키 목록) |
| API 문서 | springdoc-openapi 3.1.0 — `/blanken/swagger-ui.html` (`server.servlet.context-path: /blanken`) |
| 검증 | `spring-boot-starter-validation` — 요청 DTO에 `@Valid` |
| 쿼리 빌더 | QueryDSL (`querydsl-jpa` + `querydsl-apt`, **`jakarta` 분류자 필수**). 버전은 부트 BOM이 관리(5.1.0)하므로 명시하지 않는다. Q클래스 생성에 `kotlin("kapt")` 플러그인 필요 |
| 테스트 | JUnit5 + MockK 1.14.11 + springmockk 5.0.1 (둘 다 부트 BOM 미관리 — 버전 직접 명시) + **Testcontainers 2.0.5** (PostgreSQL) |
| 시크릿 | 루트의 `application-secret.yaml`(gitignore) 을 `spring.config.import: optional:` 로 읽는다. 템플릿은 `application-secret.yaml.example`. 키 이름이 환경변수와 같아서 운영에서는 OS 환경변수만으로 동작한다 |
| 패키지 루트 | `io.github.ddogga.blanken` |

### 2.2 코드 구성

**계층 구조**: `controller` → `service` → `repository` → `domain`. 타입별 패키지 분리.
엔티티는 컨트롤러 밖으로 나가지 않는다 — 요청·응답은 항상 `dto`.

- `config/` — `SecurityConfig`, `OpenApiConfig`, `JpaAuditingConfig`, `QuerydslConfig`(`JPAQueryFactory` 빈), `PasswordEncoderConfig`(소셜 전용 전환 후 **미사용**)
- `config/auth/` — 인증 일체
  - `service/CustomOAuth2UserService` — 제공자 응답 → `(provider, provider_id)` 로 조회, 없으면 GUEST/PENDING 으로 생성, WITHDRAWN 이면 거부
  - `handler/Oauth2LoginSuccessHandler` — Refresh Token을 **HttpOnly 쿠키(`Path=/auth`)** 로 발급 후 상태별로 `app.oauth2.redirect.{signup|success|failure}` 로 리다이렉트
  - `repository/HttpCookieOAuth2AuthorizationRequestRepository` + `jwt/Oauth2AuthorizationRequestStore` — 인가 요청을 세션 대신 Redis에(쿠키에는 UUID 키만, TTL 3분, `GETDEL`)
  - `jwt/` — `JwtTokenProvider`, `JwtAuthenticationFilter`(빈 등록 안 함 — 서블릿 필터 이중 등록 방지), `AuthTokenService`(재발급), `RefreshTokenStore`(Redis, `GETDEL` 로 Rotation), EntryPoint(401)/AccessDeniedHandler(403)
  - `AuthErrorCode` — 필터 단계 인증 실패 코드 (§4.10)
- `domain/` — 하위 패키지로 나뉜다: `user/`(`User`, `Follow`, `UserRole`, `UserStatus`), `quiz/`(`QuizSet`, `Quiz`, `Category`, `QuizSetLike`, `Visibility`, `QuizSetOrderEnum`), `history/`(`StudyHistory`, `StudyHistoryDetail`), 루트에 `BaseTimeEntity`, `OauthProvider`(제공자별 응답 파싱 enum)
- `repository/` — 도메인별 JPA 리포지토리 + `querydsl/` 커스텀 구현(`QuizSet`·`QuizSetLike`·`Follow`)
- `service/` — `UserService`, `QuizSetService`, `QuizService`, `QuizSetLikeService`, `StudyHistoryService`, `StudyHistoryDetailService`, `FollowService`
- `controller/` — `AuthController`(`/auth/refresh`, `/auth/logout`), `UserController`, `QuizSetController`, `QuizController`, `StudyHistoryController`, `FollowController`
- `dto/` — 도메인별 하위 패키지 + `common/`(`PageResponse`, `CursorResponse`)
- `exception/` — `ErrorCode` + `BusinessException` + 도메인 예외 + `GlobalExceptionHandler`

**회원 모델** — 소셜 로그인/가입만 허용한다(2026-10-04). 비밀번호 컬럼이 없고, 회원은 **`(provider, provider_id)` UNIQUE** 로 식별한다. 이메일은 제공자가 다르면 중복을 허용한다.
상태 전이: 소셜 인증 직후 `GUEST/PENDING` → `POST /api/users/signup`(약관·닉네임) → `USER/ACTIVE`. 탈퇴는 소프트 삭제(`WITHDRAWN` + `withdrawn_at`).

**인증 흐름 요약** — 상세 시퀀스는 `README.md`.
1. `GET /oauth2/authorization/{provider}` → 제공자 동의 → 콜백 → Refresh 쿠키 발급 + 프론트로 리다이렉트
2. 프론트가 `POST /auth/refresh` 로 첫 Access Token을 받는다. 만료(`TOKEN_EXPIRED`) 시에도 같은 API로 재발급 (Rotation)
3. 이후 `Authorization: Bearer {accessToken}`. 필터는 DB 조회 없이 클레임(`sub`, `role`)만으로 `AuthUser(userId, userRole)` 를 만든다 → 컨트롤러에서 `@AuthenticationPrincipal authUser: AuthUser`

### 2.3 개발 데이터 · 테스트 리소스

- `src/main/resources/data.sql` — 개발용 시드. **여러 번 실행해도 안전**하게 `ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS` 로 작성한다. 시드 유저는 `provider_id` 가 `seed-` 로 시작한다.
  - 유저 6명(상태별) + 팔로우용 유저 25명(`follow1~25@blanken.io`, 22번 PENDING·23번 WITHDRAWN·24~25번 대조군), 카테고리 10, 퀴즈셋 100
  - `test@test.io` — 좋아요 30건, **23명 팔로잉**(ACTIVE 조회 결과 21건)
  - `blanken@blanken.io` — **23명의 팔로워**(ACTIVE 조회 결과 21건)
- `src/test/resources/data.sql` — **main 과 동일한 내용을 유지한다.** Repository 테스트가 `@Sql("/data.sql")` 로 매 테스트 전에 실행하므로 멱등성이 깨지면 테스트가 깨진다.
- `schema.sql`(main/test) — `pg_trgm` 확장 + `idx_quiz_set_title_trgm`. Hibernate가 만들지 못하는 부분만 담는다. Flyway 도입 시 마이그레이션으로 옮길 대상.

**아직 없는 것** (필요해지면 추가): WebSocket, 메시지큐 클라이언트, 마이그레이션 도구(Flyway), Dockerfile·docker-compose, K8s 매니페스트, CI/CD 파이프라인, 프로파일 분리(dev/prod).

### 2.4 쿼리 빌더 — Kotlin JDSL이 아니라 QueryDSL을 쓴다 (2026-09-23 결정)

Kotlin JDSL을 먼저 붙여 봤지만 **부트 4.1에서 아예 동작하지 않는다.** 실제로 띄워서 확인한 결과:

```
java.lang.ClassNotFoundException: org.springframework.data.jpa.repository.query.StringQuery
  at o.s.data.jpa.repository.query.QueryEnhancerFactoryAdaptor.forQuery(...)
     ~[spring-data-jpa-support-3.5.5.jar]
  at c.l.kotlinjdsl.support.spring.data.jpa.JpqlEntityManagerUtils.createEnhancedQuery
```

JDSL의 `spring-data-jpa-support`는 스프링 내부에 접근하려고 **`org.springframework.data.jpa.repository.query` 패키지에 자기 클래스(`QueryEnhancerFactoryAdaptor`)를 끼워 넣어** 배포한다. 그 클래스가 Spring Data JPA 4에서 제거된 `StringQuery`를 참조해서 터진다. `findPage`뿐 아니라 `findAll`도 전부 `createEnhancedQuery`를 거치므로 우회할 수 없고, **3.5.5가 최신이라 버전을 올려서 해결되지도 않는다.**

QueryDSL은 `EntityManager.createQuery`라는 **공개 API만** 쓴다. 그래서 Spring Data 버전이 올라가도 영향을 받지 않는다. 이게 두 라이브러리의 구조적 차이고, 이번 선택의 실질적인 근거다 — 문법 취향이 아니라 **프레임워크 내부에 의존하느냐**의 문제다.

> 쿼리 빌더는 **동적 조건자**(키워드·카테고리 등 선택적 필터 조합)를 위한 것이지, 컬렉션 fetch join + 페이징의 메모리 문제(`HHH90003004 ... applying in memory`)를 풀어주지 않는다. 그건 빌더가 아니라 쿼리 모양(id만 페이징 → 그 페이지만 fetch join)으로 해결한다.

> **주의**: `ddl-auto: update`는 M1 한정. 컬럼 삭제·타입 변경을 반영하지 못하고 운영에 쓸 수 없다. **인프라 구축과 함께 Flyway로 이전한다** (§9). 이전 후에는 `validate` 로 바꿔 엔티티와 스키마 불일치를 기동 시점에 잡는다.
> Redis 의존성은 들어왔지만 **진행 상태·이어풀기(§4.3·4.4)는 아직 미구현**이다. 학습 기록은 현재 완주 결과를 `POST /api/study-histories` 로 바로 받는 형태만 있다.

---

## 3. 아키텍처 결정

- **모듈러 모놀리스 + 알림 서비스만 별도 프로세스.** MSA 전면 분해는 이 규모에 오버스펙. 알림은 팬아웃 버스트 특성과 결합도가 본체와 명확히 달라서만 분리하고, 메시지큐로 느슨하게 결합한다. K8s에서 각각 Deployment로 두어 독립 스케일링.
- **저장소 역할 분담**
  - **RDBMS** — 정합성 기준(source of truth): 유저, 퀴즈셋/퀴즈, 팔로우, 좋아요, 학습 기록, 대결 결과 (진행 상태는 §4.3에 따라 Redis 전용)
  - **Redis** — 가속 계층·휘발성 상태: Refresh Token·OAuth2 인가 요청(구현됨), 좋아요 카운터, 인기 랭킹(ZSET), 학습 진행 세션(TTL), 대결 방 상태·선착순 판정·라이브 스코어보드
  - **메시지큐** — 퀴즈셋 생성 알림 팬아웃, 대결 종료 후처리, (도입 시) RDB↔ES 동기화
- **알림 채널 이원화**: 앱이 켜져 있을 때의 실시간 UI 갱신(대결 스코어보드 등)은 **WebSocket**, 백그라운드/종료 상태에도 도착해야 하는 푸시는 **FCM**(웹은 FCM Web Push). 모바일에서 백그라운드 소켓은 유지되지 않고, 웹도 탭이 닫히면 마찬가지다.

---

## 4. 도메인 핵심 규칙 (구현 시 반드시 지킬 것)

### 4.1 채점은 클라이언트에서 한다
- "제출" 버튼 없이 입력값이 정답과 일치하면 즉시 정답 처리하는 UX → 정답이 클라이언트에 있어야 함. 세션 시작 시 정답을 **프리페치**한다.
- 서버는 일반 학습에서 **판정자가 아니라 결과 보고를 받는 쪽**이다.
- **예외**: 조작 가능성이 있는 **실시간 대결은 서버 채점**. 좋아요·랭킹 등 신뢰가 필요한 값도 서버 권위 유지.
- 관리 포인트: 정답 정규화 규칙(대소문자·공백·유니코드)의 **단일 출처** 유지 — 클라/서버 병행 시 불일치 방지.

### 4.2 오답의 정의
- 오답 입력 시 정답 여부만 알리고 **무제한 재입력** 허용. 재시도 끝에 맞힌 문제는 **정답 취급**.
- **"포기(give-up)"를 눌러야만 오답으로 기록**된다.
- 따라서 **오답 재풀이 대상 = 포기한 문제뿐**이다. (`study_history_detail.gave_up`)

### 4.3 진행 상태는 Redis, 학습 기록은 RDB
가변 상태와 불변 기록을 저장소 자체로 분리한다.

- **진행 상태(progress) — Redis에만 존재한다. RDB 테이블 없음.**
  `study:session:{userId}:{quizSetId}` (Hash, TTL 24h). 어디까지 풀었는지·현재 문제·포기한 문제 목록 등 세션 동안만 유효한 값.
  PRD 초안의 `quiz_set_progress` 테이블은 **채택하지 않는다** (2026-08-22 결정).
- `study_history` — 완주할 때마다 **append-only, 불변**. 점수·오답 목록·완료 시각.
  최근 1건 덮어쓰기가 아니라 **누적**하는 이유: 성장 추이·오답 변화 추적이 학습 앱의 핵심 가치이고, 불변 이벤트 누적이 갱신 충돌 없이 통계에 유리하기 때문.

**이 결정에서 따라오는 조회 규칙**
- **"진행중 목록" = Redis 키 스캔이 아니라** 유저별 진행중 세션 인덱스(예: `study:active:{userId}` Set)를 함께 유지해 조회한다. 운영 중 `KEYS` 금지, 필요하면 `SCAN`.
- **"완료 목록" = `study_history`에서 유도**한다 (유저의 distinct `quiz_set_id`). 별도 status 컬럼이 없으므로 "완주한 적 있음"이 곧 완료다.
- TTL이 지나 세션이 사라지면 진행중 목록에서도 사라진다. 이는 버그가 아니라 4.4의 이어풀기 정책과 같은 규칙이다.

### 4.4 이어풀기 — TTL이 곧 정책
- 중단한 학습 세션은 **하루 이내에만** 이어풀기 가능. 지나면 처음부터.
- 근거는 자원 절약이 아니라 **학습적 판단**: 오래 방치한 뒤의 이어풀기는 복습 효과가 낮다.
- 구현: 진행 세션을 Redis TTL 24h로 저장 → 별도 만료 정리 로직 불필요, 신뢰할 수 없는 "이탈 감지" 이벤트에 의존하지 않음.
- **완주 시에만 RDB flush**(멱등) — flush 대상은 `study_history` + `study_history_detail`뿐이다. 미완료 세션은 TTL로 자연 소멸하며 RDB에 어떤 흔적도 남기지 않는다.

### 4.5 좋아요
- **정합성 기준은 DB, 평상시 읽기·쓰기는 Redis.** Redis 장애 시 DB에서 복구 가능해야 한다.
- cache miss 시 DB에서 현재 카운트를 읽어 Redis에 세팅(lazy loading). 주기적 배치 flush는 **증분 키 분리 등으로 멱등하게** 설계해 중복 증가를 막는다.
- 클라이언트는 낙관적 UI(즉시 반영 후 실패 시 롤백) → 서버는 **멱등**해야 한다. 더블탭·재시도가 와도 결과는 좋아요 1개로 수렴.

**현재 구현 (Redis 카운터 도입 전 단계)** — 카운트는 아직 Redis가 아니라 **DB 원자 UPDATE**다.
- 중복: `quiz_set_like` UNIQUE + `saveAndFlush` → `DataIntegrityViolationException` 을 `QuizSetLikeDuplicationException`(Q004)으로 변환
- 카운트: 더티 체킹 대신 `QuizSetRepository.addLikeCount` / `cancelLikeCount` 의 `like_count = like_count ± 1` 단일 UPDATE. 취소는 `like_count > 0` 조건 + 실제 삭제된 행이 있을 때만 감소
- `QuizSetLikeConcurrentTest` 가 동시 요청에서 카운트가 맞는지 검증한다.
- 위 Redis `INCR` + 멱등 flush 설계는 쓰기 부하가 문제될 때의 **고도화 단계**로 남아 있다.

### 4.6 검색
- 퀴즈셋 이름 + 카테고리 복합 검색. **퀴즈셋당 카테고리는 1개**지만(§4.7) 사용자는 여러 개를 골라 거를 수 있다 — `WHERE quiz_set.category_id IN (...)`. 다대일이 되면서 조인 없이 `quiz_set` 한 테이블에서 끝나고, 퀴즈셋이 여러 행으로 불어나지 않아 `DISTINCT` 도 필요 없다.
- **서버 필터링(재쿼리) 채택.** 필터 토글 시 클라가 보유한 결과를 거르지 않고 서버에 재요청한다. 페이지네이션 위에서의 클라 필터링은 로드된 부분집합만 걸러 부정확하기 때문.
- **검색 결과에 좋아요 정보 병합**하되 N+1 금지: isLiked는 `WHERE user_id=? AND quizset_id IN (...)` 한 번, 카운트는 Redis `MGET` 한 번.
- ES + Nori는 **속도 때문이 아니라** 형태소 분석·관련도 랭킹·오타 허용·패싯이 필요해질 때 도입. 속도만 필요하면 `pg_trgm`으로 충분. 도입 시 RDB↔ES 동기화는 이벤트/MQ(또는 CDC).

### 4.7 퀴즈셋 CRUD
- 2단계 생성 플로우: ① 이름·설명·카테고리로 퀴즈셋 생성(POST) → ② 수정 페이지에서 퀴즈 추가/삭제/수정.
- 퀴즈 수정은 PUT. 클라이언트는 **변경이 발생했을 때만** 수정 버튼을 활성화해 호출한다.
- 퀴즈 생성 입력: 정답 단어가 포함된 영어 문장 + 정답 단어 + (선택) 힌트.
- **퀴즈셋은 카테고리 하나에 속한다 — `QuizSet.category` 는 `@ManyToOne(optional = false)`** (2026-09-23 결정).
  원래는 다대다(`quiz_set_category` 조인 엔티티 + 복합 PK)였으나 다대일로 정리했다. 조인 엔티티·`QuizSetCategoryId`·`addCategory`/`updateCategories` 가 모두 사라졌다.
  - **"카테고리가 없으면 안 된다"는 규칙(2026-09-01 결정)은 그대로 유효하다.** 카테고리 없는 퀴즈셋은 §4.6 필터 검색에 영원히 잡히지 않아, 만들어지는 순간 발견될 수 없는 데이터가 된다. 바뀐 건 규칙이 아니라 **강제 수단**이다.
  - 이제 `quiz_set.category_id` 가 **NOT NULL FK** 라 스키마가 보장한다. 애플리케이션 검증은 메시지 품질용 방어선이지 보장이 아니다 — §6의 UNIQUE 제약과 같은 구도.
  - 사용자 입력은 `QuizSetCreateRequest.categoryId` 의 **`@Positive`** 가 거른다. 필드를 통째로 빼면 Kotlin 논-널이라 `@Valid` 가 돌기 전에 역직렬화에서 걸려 **`fieldErrors` 없는 `C001`** 이 나간다 (`ownerId` 와 같은 처리).
  - `@Size(min = 1)` 을 쓰지 않는 이유: 그 애너테이션이 필요했던 건 리스트가 **"있는데 비어 있는"** 상태로 역직렬화를 통과할 수 있어서였다. 스칼라 `Long` 에는 그 상태가 없다.
  - **"엔티티는 이 규칙을 강제하지 않는다"(2026-09-02 결정)는 뒤집혔다.** 그때 못 막은 이유는 카테고리가 `addCategory` 로 **생성 후에** 붙는 구조여서 생성자로는 손댈 수 없었기 때문인데, 다대일이 되면서 카테고리가 생성자 논-널 파라미터로 들어온다. 팩터리나 `private` 생성자 없이 강제되므로 **테스트에서 `id` 를 지정하는 길도 그대로 남는다.** 당시 규칙을 포기하게 만든 거래 자체가 사라졌다.

### 4.8 팔로우 + 알림 팬아웃
- **팔로우 자체는 구현됨** — `follow(follower_id, followee_id)` UNIQUE, 자기 자신 팔로우는 엔티티 `init` 에서 거부. 팔로우마다 알림 수신 플래그(`notify_quiz_set_created`, `notify_quiz_battle_opened`)를 둬서 팬아웃 시 수신 거부를 거른다.
- 팔로잉/팔로워 목록은 **ACTIVE 유저만** 노출하고 **커서 페이징**(§4.10)이다.
- 알림 팬아웃(아래)은 미구현.
- 생성 API는 `QuizSetCreated` 이벤트만 발행하고 **즉시 응답**. 알림 서비스 컨슈머가 팔로워 목록을 조회해 알림 생성/전송, 실패 시 재시도(DLQ).
- 절대 생성 API의 트랜잭션 안에서 팔로워 전체에 동기 전송하지 않는다.

### 4.9 실시간 대결 (선착순)
- 한 퀴즈셋으로 여러 참가자가 문제를 순차 진행, 각 문제를 **가장 먼저 맞힌 한 명**이 득점.
- WebSocket 메시지: `QUESTION_START` / `SUBMIT_ANSWER` / `ROUND_RESULT` / `SCOREBOARD_UPDATE` / `GAME_OVER`. 방 상태는 Redis에 TTL 저장, **서버 주도 타이머**로 진행.
- 판정 기준은 **서버 도착 순서** — 클라이언트 타임스탬프는 믿지 않는다.
- 종료 후처리는 `BattleFinished` 이벤트로 큐에 넘겨 진행 프로세스의 응답성을 유지한다.

---

### 4.10 API 규약 (User CRUD에서 확립, 이후 도메인도 따를 것)
- **응답 상태**: 생성 `201` + `Location`, 본문 없는 성공 `204`, 조회·수정 `200`.
- **에러 응답**은 `ErrorResponse(code, message, fieldErrors?)` 하나로 통일. `code`는 클라이언트 분기용, `message`는 사람이 읽는 값.
  상태·코드·메시지의 단일 출처는 **`exception/ErrorCode` enum**이다. 응답은 직접 생성하지 말고 `ErrorResponse.of(errorCode, fieldErrors?)`로 만든다.
  **코드 체계**: `앞자리 알파벳(도메인) + 3자리 순번`. `C`=Common(횡단), `U`=User, `Q`=Quiz/QuizSet, `G`=Category, `A`=Auth. 순번은 도메인별 `001`부터, **`999`는 서버 오류·기타로 예약**.

  | enum | code | status | 비고 |
  |---|---|---|---|
  | `VALIDATION_FAILED` | `C001` | 400 | |
  | `DATA_INTEGRITY_VIOLATION` | `C002` | 409 | |
  | `INTERNAL_ERROR` | `C999` | 500 | 예약 — 아직 핸들러 없음 |
  | `USER_NOT_FOUND` | `U001` | 404 | |
  | `DUPLICATE_EMAIL` | `U002` | 409 | 소셜 전용 전환 후 **미사용** (이메일 UNIQUE 없음) |
  | `INVALID_OAUTH2_USER_INFO` | `U003` | 400 | 제공자 응답 파싱 실패 |
  | `QUIZ_SET_NOT_FOUND` | `Q001` | 404 | |
  | `QUIZ_NOT_FOUND` | `Q002` | 404 | |
  | `QUIZ_SET_TITLE_DUPLICATION` | `Q003` | 409 | 같은 소유자 내 제목 중복 |
  | `QUIZ_SET_LIKE_DUPLICATION` | `Q004` | 409 | |
  | `QUIZ_SET_ACCESS_DENIED` | `Q005` | 403 | |
  | `CATEGORY_NOT_FOUND` | `G001` | 404 | |
  | `INVALID_PASSWORD` | `A001` | 400 | 소셜 전용 전환 후 **미사용** |

  `G` = cateGory. `C` 는 Common 이 선점해 알파벳이 겹쳐서다.

- **필터 단계 인증 실패는 `config/auth/AuthErrorCode` 가 따로 담당한다.** 컨트롤러에 도달하기 전이라 `GlobalExceptionHandler` 를 거치지 않고, `writeAuthError` 가 `{"code","message"}` 를 직접 쓴다. `code` 는 enum 이름 그대로다(`UNAUTHORIZED` / `TOKEN_EXPIRED` / `INVALID_TOKEN` / `REFRESH_TOKEN_INVALID` / `ACCESS_DENIED`) — 위 `알파벳+순번` 체계와 **형식이 다르다.** 프론트 분기: `TOKEN_EXPIRED` → `/auth/refresh` 후 재시도, 나머지 401 → 재로그인.

- **도메인 예외는 `BusinessException(errorCode, detail?)`을 상속**한다. `GlobalExceptionHandler`는 이 타입 하나만 잡으므로 예외를 추가해도 핸들러를 건드릴 일이 없다.
  **클라이언트에는 `ErrorCode.message`(표준 문구)만 나간다.** `detail`(`(id=1)`, `(email=...)` 등 진단용 맥락)은 예외의 `message`에 담겨 **로그로만** 남는다 — 내부 식별자가 응답으로 새지 않게 하기 위해서다.
- **요청 본문을 읽지 못한 경우(`HttpMessageNotReadableException`)도 같은 형식으로 내보낸다** — 깨진 JSON, 필수 필드 누락, 잘못된 enum 값.
  Bean Validation 은 역직렬화가 **성공한 뒤에** 돌기 때문에, Kotlin 논-널 필드가 비면 `@Valid` 가 돌기도 전에 Jackson 이 먼저 실패한다.
  이 핸들러가 없으면 스프링 기본 에러 바디(`timestamp`/`status`/`error`/`path`)가 나가 **클라이언트가 에러 형식 두 개를 파싱**해야 한다. `VALIDATION_FAILED`(C001)로 통일하되, 파서 메시지에는 내부 타입 정보가 섞여 있어 `fieldErrors` 없이 표준 메시지만 내보낸다.
- **페이징 응답은 `PageResponse<T>`로 감싼다.** `Page`(`PageImpl`)를 그대로 직렬화하면 JSON 구조가 Spring Data 내부 구현에 묶여 버전 간 안정성이 깨진다.
- **무한 스크롤 목록은 커서 페이징 `CursorResponse<T>(content, nextCursor, hasNext)`** — 팔로우 목록에서 도입. `id DESC` 정렬 + `id < cursor`, **`size + 1` 개를 조회해 초과분으로 `hasNext` 를 판단**하고 `size` 개만 돌려준다. `nextCursor` 는 마지막 항목의 id(없으면 `null`).
- **멱등하지 않은 변경은 PUT이 아니라 POST.**
- **부분 수정은 PATCH.**
- **인증 주체는 경로·본문이 아니라 토큰에서 꺼낸다** — `@AuthenticationPrincipal authUser: AuthUser`. `UserController`(닉네임 변경·탈퇴·가입 완료), `FollowController` 는 이 방식이다.

**남아 있는 인증 부채** (인증 도입 후 아직 정리 안 됨)
- `SecurityConfig` 가 `anyRequest permitAll` 이다. `hasRole("USER")` 는 주석 처리돼 있고 프로파일 분리 후 켜기로 한 TODO 상태.
- 아래는 여전히 **요청이 지정한 id 를 신뢰**한다 — 토큰의 `userId` 로 바꾸고 소유권을 검사해야 한다.
  - 좋아요: `PUT/DELETE /api/quiz-sets/{quizSetId}/likes/{userId}`, `GET /api/quiz-sets/likes/{userId}`
  - 퀴즈셋 생성: `QuizSetCreateRequest.ownerId`
  - 학습 기록 생성: `StudyHistoryRequest.userId`
- `GET /api/users`(전체 목록)이 열려 있다 — 제한/제거 대상.
- `UserController.signup` 이 `ErrorCode` 를 거치지 않고 `ErrorResponse("SIGNUP_ALREADY_COMPLETED", ...)` 처럼 **임의 코드 문자열을 직접 만든다** — 위 단일 출처 규약에서 벗어나 있다.

---

## 5. 테스트 코드 작성 기준

> 판단이 애매하면 **임의로 정하지 말고 작성 전에 질문한다.** 테스트를 과잉 생성하는 것보다 확인하고 진행하는 편이 낫다. 질문이 필요한 상황은 5.6에 정리해 두었다.

### 5.1 테스트 스택
- **JUnit5 + MockK**. 슬라이스 테스트는 `@WebMvcTest`(Controller), 필요 시 `@DataJpaTest`(Repository).
- **Kotest 전환 가능성을 염두에 둔다.** 특정 러너에 강하게 결합된 구조는 피하고, given-when-then의 논리적 구분을 주석이나 구조로 드러내 전환 시 마찰을 줄인다.

- `@WebMvcTest`에서 빈 교체는 **springmockk의 `@MockkBean`**을 쓴다 (부트 기본 `@MockitoBean`은 Mockito 전용). 5.0.1 + 부트 4.1 조합 동작 확인 완료.
- `@WebMvcTest` 는 실제 `SecurityConfig` 대신 **`config/TestSecurityConfig`**(CSRF off + `permitAll`)를 `@Import` 한다. 인증 주체가 필요한 테스트는 `spring-security-test` 로 주입한다.
- **Repository 테스트와 동시성 테스트는 Testcontainers 로 실제 PostgreSQL 을 띄운다** (`support/PostgresTestContainerConfig`, `postgres:17-alpine`, `@ServiceConnection`). `ilike`·`pg_trgm` 같은 PostgreSQL 전용 문법은 H2로 검증할 수 없기 때문이다.
  - **Docker 데몬이 꺼져 있으면** `Could not find a valid Docker environment` → `NoClassDefFoundError: Could not initialize class PostgresTestContainerConfig` 로 해당 테스트들이 줄줄이 실패한다. 코드 문제가 아니라 Docker Desktop을 켜면 된다.
  - pgAdmin 접속용으로 호스트 포트를 **`55432` 로 고정**해 두었다. 포트가 점유돼 있으면 기동이 실패하고, 테스트 병렬 실행이 불가하다.
  - 테스트 데이터는 `@Sql("/schema.sql", BEFORE_TEST_CLASS)` + `@Sql("/data.sql")`(§2.3), 동시성 테스트는 `@Sql("/user.sql")`.

> **주의**: 부트 4에서 테스트 애너테이션 패키지가 옮겨졌다. `@WebMvcTest`는 `org.springframework.boot.test.autoconfigure.web.servlet`이 아니라 **`org.springframework.boot.webmvc.test.autoconfigure`**다. 부트 3 기준 예제를 그대로 붙이면 `Unresolved reference`가 난다.

### 5.2 계층별 전략
| 계층 | 방침 |
|---|---|
| Controller / Service | 하나의 로직에 대해 **기본적으로 작성한다** |
| Repository | **QueryDSL 등 쿼리 빌더가 필요한 복잡한 쿼리일 때만.** 단순 파생 쿼리(`findById`, `findByEmail` 등)는 작성하지 않는다 |

### 5.3 작성 생략 기준
- ID 단건 조회처럼 **분기·가공 없이 위임만 하는 자명한 로직**은 테스트하지 않는다.
- 1차 판단 기준은 **"성공 케이스를 테스트할 가치가 있는가"**. 성공 케이스조차 자명하면 네거티브도 만들지 않는다.

### 5.4 네거티브 테스트 추가 기준
**실패를 내 코드가 직접 책임지는가**로 판단한다.

**추가한다**
- 도메인 규칙 위반으로 **내가 예외를 던지는 분기** (중복 이메일, 잔액 부족 등)
- 조회 실패를 도메인 예외로 변환하는 지점 (`orElseThrow` 등)
- 예외 → HTTP status/code 변환이 올바른지 (Controller 계층)
- 입력 검증 실패가 단순 거절이 아니라 **별도 분기·흐름을 만드는** 경우

**추가하지 않는다**
- Bean Validation 애너테이션 자체의 동작 (`@NotNull`, `@Email` 등 — 프레임워크 보장 영역)
- 도메인 예외로 변환하지 않는 순수 DB 제약 위반 (FK/UNIQUE)
- Kotlin 널 세이프티 등 컴파일 단계에서 걸리는 것

원칙: **성공 케이스가 있는 로직에 한해**, "내가 명시적으로 처리한 실패 분기"가 있으면 네거티브 테스트를 짝으로 붙인다.

### 5.5 테스트명 규칙
공통 규칙은 **한글**로 쓰고 띄어쓰기는 `_`로 구분하는 것. 그 위에서 **계층마다 형식이 다르다.**

**Controller — `응답코드_테스트내용_성공|실패`**

컨트롤러 테스트의 관심사는 HTTP 계약이므로 **상태 코드를 맨 앞에 둔다.** 테스트 목록만 훑어도 어떤 상태 코드가 커버돼 있는지 바로 보이게 하기 위해서다. 끝은 `성공` 또는 `실패`로 닫는다.

```
201_유저_생성_성공
409_중복_이메일_가입_실패
400_유저생성_잘못된_형식의_요청_실패
404_존재하지_않는_유저_조회_실패
200_닉네임_변경_성공
204_비밀번호_변경_성공
400_비밀번호_변경_현재_비밀번호_불일치_실패
204_유저_삭제_성공
```

- 같은 상태 코드가 여러 개면 **어느 API인지를 내용 앞에 붙여** 구분한다 (`400_유저생성_...` / `400_비밀번호_변경_...`).
- **에러 코드(`U002`, `A001` 등)는 이름에 넣지 않는다.** 이름은 상태 코드까지만 드러내고, 에러 코드는 어서션에서 검증한다 — 이름과 어서션이 같은 말을 두 번 하지 않도록.

**Service·그 외 — 서술형 "조건 + 기대 결과"**

상태 코드가 없는 계층이라 무엇을 하면 무엇이 되는지를 문장으로 쓴다. 실패 케이스는 **던지는 예외 이름**을 드러낸다.

```
유저를_정상적으로_생성한다
존재하지_않는_유저_조회시_USER_NOT_FOUND_예외를_던진다
중복된_이메일로_가입시_DUPLICATE_EMAIL_예외를_던진다
```

### 5.6 판단이 애매할 때 — 질문할 것
아래는 기준만으로 갈리지 않으므로 **작성 전에 확인한다.**
- 특정 로직이 "자명해서 생략"(5.3) 대상인지 경계가 모호할 때
- Repository 테스트가 필요한 "복잡한 쿼리"(5.2)인지 판단이 서지 않을 때
- 네거티브 테스트(5.4)를 붙일지 기준만으로 가려지지 않을 때

---

## 6. 동시성 제어 — 이 프로젝트의 핵심 축

> **Redis 싱글스레드는 "명령 하나의 원자성"만 보장한다. 여러 명령에 걸친 논리적 원자성은 UNIQUE 제약·SETNX·Lua 스크립트로 별도 설계한다.**

| 지점 | 문제 | 해법 | 상태 |
|---|---|---|---|
| 소셜 계정 중복 가입 | 같은 제공자 계정으로 동시 가입 | `users (provider, provider_id)` **UNIQUE 제약**. (이메일 UNIQUE는 소셜 전용 전환으로 제거 — 제공자가 다르면 같은 이메일 허용) | ✅ 제약 |
| 좋아요 중복 | check-then-act 경쟁 | `(user_id, quiz_set_id)` **UNIQUE** + `saveAndFlush` 로 즉시 INSERT → 위반을 Q004 로 변환 (`save` 만 하면 flush가 커밋 시점으로 밀려 catch 밖에서 터진다) | ✅ |
| 좋아요 카운트 | 갱신 유실 | 현재: DB 단일 UPDATE `like_count = like_count ± 1` (§4.5). 고도화: Redis 원자 `INCR` + 멱등 flush | ✅ / 예정 |
| 팔로우 중복 | check-then-act 경쟁 | `(follower_id, followee_id)` **UNIQUE** | ✅ 제약 |
| Refresh Token 재사용 | 같은 토큰으로 동시 재발급 | Redis **`GETDEL`** — 한 요청만 성공 (Rotation) | ✅ |
| OAuth2 콜백 중복 | 같은 인가 요청 콜백 2회 | Redis **`GETDEL`** — 인가 요청 일회용 | ✅ |
| 대결 선착순 | 최초 정답자 1인 확정 | `winner:{roomId}:{questionId}` 에 `SETNX` — first-writer-wins | 예정 |
| 대결 방 정원 | 정원 초과 동시 입장 (재고 문제와 동형) | Redis **Lua** 원자 check-and-increment | 예정 |

---

## 7. 데이터 모델

**RDBMS**
엔티티는 `io.github.ddogga.blanken.domain` 하위 패키지(`user/`, `quiz/`, `history/`)에 **구현 완료**. ERD는 `README.md`.

```
users                 (id, email, nickname, provider, provider_id, user_status, user_role,
                       terms_agreed_at, marketing_agreed, withdrawn_at, created_at, updated_at)
                                                          -- UNIQUE(provider, provider_id), 비밀번호 컬럼 없음
quiz_set              (id, owner_id, category_id, title, description, visibility, like_count, quiz_count, created_at, updated_at)
category              (id, name)                          -- name UNIQUE
quiz                  (id, quiz_set_id, sentence, answer_word, hint, created_at, updated_at)
                                                          -- sentence 는 빈칸 `{{}}` 이 정확히 하나 (엔티티 init 검증)
quiz_set_like         (id, user_id, quiz_set_id, created_at, updated_at)  -- UNIQUE(user_id, quiz_set_id)
study_history         (id, user_id, quiz_set_id, score, total_count, correct_count, solved_at)  -- append-only
study_history_detail  (id, history_id, quiz_id, gave_up)  -- UNIQUE(quiz_id, history_id)
follow                (id, follower_id, followee_id, notify_quiz_set_created, notify_quiz_battle_opened,
                       created_at, updated_at)            -- UNIQUE(follower_id, followee_id)

-- 이후 마일스톤
battle_result / battle_result_player
```

`user_status` = `PENDING` / `ACTIVE` / `WITHDRAWN`, `user_role` = `GUEST` / `USER` / `ADMIN` (§2.2 상태 전이).

**테이블·컬럼 명명 주의**
- `user`, `like`는 PostgreSQL 예약어 → `users`, `quiz_set_like`로 사용한다.
- **`quiz_set_progress` 테이블은 존재하지 않는다.** 진행 상태는 Redis 전용이다 (4.3 참고).

**공통 규약**
- `BaseTimeEntity`(`@MappedSuperclass`) — `created_at` / `updated_at`을 Spring Data JPA Auditing으로 채운다. `@EnableJpaAuditing`은 `config/JpaAuditingConfig.kt`.
- 모든 `@ManyToOne`은 **`FetchType.LAZY` 명시**. 기본값 EAGER는 N+1의 주원인.
- enum 컬럼(`visibility`, `provider`, `user_status`, `user_role`)은 `@Enumerated(EnumType.STRING)` — ordinal은 enum 순서가 바뀌면 데이터가 깨진다.
- 컬렉션은 내부 `MutableList` + 읽기 전용 `List` 노출 + `addXxx()` 로 양방향 연관 세팅.
- `quiz_set.like_count`는 `protected set` — 엔티티 메서드로는 바꾸지 않는다. 현재는 리포지토리의 원자 UPDATE 쿼리로만 증감하고(§4.5), Redis 카운터 도입 후에는 배치 flush로만 갱신된다.

**스키마 마이그레이션 (Flyway, `db/migration/V1__init_schema.sql`)**
- **enum 컬럼의 CHECK 제약은 유지한다** (2026-10-13 결정). Hibernate가 `@Enumerated(STRING)` 컬럼마다 `CHECK (col IN (...))` 를 생성했고 V1에 그대로 들어 있다 (`quiz_set_visibility_check`, `users_provider_check`, `users_user_role_check`, `users_user_status_check`).
  - 근거: 잘못된 값을 애플리케이션이 아니라 DB가 최종적으로 거부한다 — §6의 UNIQUE 제약과 같은 구도.
  - **대가 — enum 상수를 추가·변경할 때는 CHECK를 바꾸는 마이그레이션(`V{n}__...sql`)을 반드시 같은 배포에 포함한다.** 빠뜨리면 컴파일·`validate` 는 통과하는데(Hibernate `validate` 는 CHECK 를 보지 않는다) **운영에서 새 값을 INSERT/UPDATE 하는 순간 실패**한다. 예: 제공자 `GOOGLE` 추가 → `users_provider_check` 를 DROP 후 재생성.
- **FK 컬럼 인덱스는 V1에 넣지 않았다 — 보류** (2026-10-13 결정). PostgreSQL은 FK에 인덱스를 자동 생성하지 않고, 엔티티에 `@Index` 가 없어 Hibernate도 만들지 않았다. 현재 인덱스는 PK·UNIQUE·`idx_quiz_set_title_trgm` 뿐이다.
  - 인덱스 없이 풀스캔되는 조회: `follow.followee_id`(팔로워 목록 — UNIQUE `(follower_id, followee_id)` 는 선두 컬럼이 달라 못 탄다 → 후보 `(followee_id, id)`), `quiz.quiz_set_id`, `quiz_set.category_id`, `quiz_set.owner_id`(+`title` 중복 검사), `study_history.user_id`/`quiz_set_id`.
  - UNIQUE 선두 컬럼으로 이미 커버되는 것: `follow.follower_id`(팔로잉 목록), `quiz_set_like.user_id`.
  - 추가할 때는 V1을 고치지 말고 **새 마이그레이션(V2 이후)** 으로 넣는다 — V1은 공유 DB에 한 번 적용되면 체크섬 때문에 수정할 수 없다.

**Redis 키**
```
-- 구현됨
refresh_token:{jti}                      (String = userId, TTL 14d)  -- Rotation, GETDEL
oauth2_auth_request:{uuid}               (String = 직렬화된 인가 요청, TTL 3m)  -- GETDEL

-- 예정
like:count:{quizSetId}
ranking:quizset                          (ZSET)
study:session:{userId}:{quizSetId}       (Hash, TTL 24h)  -- 진행 상태 = 이어풀기 세션
study:active:{userId}                    (Set)            -- "진행중 목록" 조회용 인덱스
battle:room:{roomId}
battle:score:{roomId}                    (ZSET)
winner:{roomId}:{questionId}             (SETNX)
battle:room:{roomId}:count
```

**메시지큐 이벤트**: `QuizSetCreated`(알림 팬아웃), `BattleFinished`(히스토리·알림), (ES 도입 시) `QuizSetChanged`

---

## 8. 마일스톤

1. **M1 — 기반**: 인증 · Quiz Set CRUD · 학습 풀이(클라 채점·재시도/포기·오답 재풀이) · 히스토리 2테이블
   - ✅ 소셜 로그인 + JWT(Refresh Rotation) · 가입 완료/탈퇴 · 퀴즈셋/퀴즈 CRUD · 학습 기록 생성
   - ⬜ 학습 풀이 세션(진행 상태 Redis, §4.3) · 오답 재풀이 · 인가 규칙 활성화와 id 신뢰 부채 정리(§4.10)
2. **M2 — 소셜**: 좋아요(Redis 카운터·UNIQUE·DB 정합성) · 검색(서버 필터링·좋아요 병합) · 팔로우 · 알림(MQ + 알림 서비스 + FCM)
   - ✅ 좋아요(DB 원자 UPDATE 단계) · 검색(키워드+카테고리, 최신/인기순, `pg_trgm`) · 팔로우/언팔로우 · 팔로잉/팔로워 커서 조회
   - ⬜ 좋아요 Redis 카운터 · 검색 결과 isLiked 병합 · 알림 팬아웃
3. **M3 — 실시간 대결**: WebSocket · 선착순(SETNX) · 방 정원(Lua) · 스코어보드 · 종료 후처리
4. **M4 — 고도화·운영**: 이어풀기(Redis TTL) · 검색 ES/Nori · Docker · Kubernetes · AWS

**Out of Scope**: 실결제/유료 콘텐츠, 양면시장, AI 문장 자동 생성, 크로스 디바이스 이어풀기.

---

## 9. 미결정 사항

구현에 착수하기 전에 확인이 필요한 것들. 결정되면 이 문서를 갱신할 것.

### 9.1 다음 작업 — 인프라 (2026-10-12 착수 예정)
Docker 이미지 빌드 → Flyway 로 DB 스키마 관리 → AWS 에 CI/CD 배포. 우선 EC2 등 AWS 리소스를 만들어 두는 단계부터 시작한다.
구성 서비스·배포 방식은 **아직 결정 전**이다. 결정되면 §2·§3에 반영할 것. 착수 시 함께 정해야 할 것:
- Flyway 전환 시 기준 스키마(V1) 작성 방법, `schema.sql`(pg_trgm·GIN 인덱스)의 마이그레이션 편입, `ddl-auto` → `validate`
- `data.sql` 시드의 운영 제외 (프로파일 분리 — `SecurityConfig` 의 `permitAll` TODO 와 같은 작업)
- 시크릿 주입 방식 (현재 `application-secret.yaml` / 환경변수 → 운영 환경의 저장소)
- OAuth2 `redirect-uri` 와 `app.oauth2.redirect.*`, `app.cors.allowed-origins` 의 운영 도메인

### 9.2 도메인·기능
- ~~**인증 방식** — JWT vs Redis 세션~~ → **해소**: JWT Access(30m) + Redis 저장 Refresh(14d, Rotation). WebSocket 핸드셰이크 인증도 이 Access Token 기준으로 설계한다.
- **네이티브 클라이언트 로그인 진입** — 현재는 서버 주도 `oauth2Login` + 프론트 URL 리다이렉트 + Refresh 쿠키(§1 간극). 안드로이드에서 그대로 쓸지(커스텀 탭 + 딥링크 리다이렉트), 클라이언트 SDK로 받은 제공자 토큰을 서버가 검증·교환하는 방식을 추가할지. §1의 "한 가지로 통일" 원칙과 함께 판단할 것.
- **탈퇴 후 재가입 쿨타임** — `User.withdrawnAt` 에 "24시간 재가입 제한" TODO 가 있다. 현재는 WITHDRAWN 이면 로그인 자체를 거부하고, 쿨타임 경과 후 처리가 없다.
- **퀴즈셋당 평균 문제 수** — 이어풀기와 Redis 도입 정당성에 직결. 10개 남짓이면 이어풀기 가치가 약해지고, 50~100개면 Redis 세션이 확실히 정당화된다.
- **빈 퀴즈셋 처리** — 2단계 생성 플로우의 부작용. draft 상태를 `quiz_set`에 넣을지를 스키마 확정 전에 정해야 한다.
- **Redis 장애 시 진행 상태 정책** — 진행 상태가 Redis 전용이 되면서 Redis 유실 = 진행중 세션 전멸이다. 애초에 24h 안에 사라질 값이라 감수 가능한 손실로 보지만, 사용자 안내 문구는 필요하다.
- **이어풀기 만료 기준** — 24h 롤링 vs 자정 기준 / 이어풀기 시작 시 TTL 갱신 여부 / 만료 안내 문구.
- **학습 기록 상한** — 유저·퀴즈셋당 기록 상한 또는 오래된 기록 요약 정책 (초기엔 무제한 append로 시작 가능). 오답 재풀이 대상 범위(최근 시도의 오답 vs 누적 포기 문제 전체).
- **채점 UX 세부** — 실시간 대조 vs 확인 시 대조, 조기 정답 확정 처리.
- **ES 도입 시점**.
- RDBMS는 PostgreSQL로 사실상 확정(build.gradle + application.yaml). PRD의 "MySQL/PostgreSQL 선택" 이슈는 해소된 것으로 본다.
