# CLAUDE.md — blanken 서버

영어 빈칸채우기 퀴즈 학습 앱(안드로이드)의 **백엔드 API 서버**.

- **원본 기획서**: `/Users/user/Documents/code/blanken_document/PRD_퀴즈학습앱_v2.md` (v0.2, 2026-08-20)
  이 문서는 그 PRD에서 **서버 구현에 영향을 주는 결정만** 추린 요약본이다. 세부 배경·근거가 필요하면 PRD를 직접 읽을 것.
- 사용자와의 대화는 한국어로 진행한다.

---

## 1. 제품 개요

사용자가 직접 만든 영어 문장으로 빈칸 채우기 퀴즈셋을 만들어 공유하고, 다른 유저와 실시간 선착순 대결까지 하는 학습 앱.
토이 프로젝트가 아니라 **실제 배포·운영되는 B2C 서비스**를 지향한다.

**핵심 원칙**: 목표 스택(Redis / 메시지큐 / 동시성 제어 / K8s)을 **도메인이 실제로 요구하는 지점에만** 적용한다.
"기술을 억지로 끼워 넣지 않고, 필요 없는 곳에는 쓰지 않는 판단"까지 설계의 일부다. 새 기술 도입을 제안할 때는 도메인 근거를 함께 제시할 것.

**클라이언트**: 안드로이드 네이티브(Kotlin), 클라이언트 렌더링. 서버는 JSON API만 제공하며 빌드·배포 파이프라인이 분리된다.
**전제**: 로그인 없이는 앱 진입이 불가능하다. 모든 API는 인증된 사용자를 전제한다.

---

## 2. 현재 저장소 상태

M1 진행 중 — 엔티티 계층까지 완료.

| 항목 | 값 |
|---|---|
| 언어/런타임 | Kotlin 2.3.21, Java 21 toolchain |
| 프레임워크 | Spring Boot 4.1.0 (`spring-boot-starter-webmvc`, `-data-jpa`) |
| DB | PostgreSQL (`ddl-auto: update`, `open-in-view: false`) |
| API 문서 | springdoc-openapi 3.1.0 — `/swagger-ui.html` |
| 검증 | `spring-boot-starter-validation` — 요청 DTO에 `@Valid` |
| 비밀번호 해싱 | `spring-security-crypto`의 BCrypt (**starter-security 아님** — 필터체인 미적용) |
| 테스트 | JUnit5 + MockK 1.14.11 + springmockk 5.0.1 (둘 다 부트 BOM 미관리 — 버전 직접 명시) |
| 패키지 루트 | `io.github.ddogga.blanken` |

기존 코드
- `config/` — `OpenApiConfig`, `JpaAuditingConfig`, `PasswordEncoderConfig`
- `domain/` — `BaseTimeEntity`, `User`, `QuizSet`, `Quiz`, `Category`, `QuizSetCategory`, `QuizSetLike`, `StudyHistory`, `StudyHistoryDetail`, `Visibility`
- `repository/` — `UserRepository`
- `service/` — `UserService`
- `controller/` — `UserController`, `TestController`
- `dto/user/`, `dto/common/PageResponse`, `exception/` (`ErrorCode` + `BusinessException` + 도메인 예외 + `GlobalExceptionHandler`)
- 테스트 — `UserServiceTest`(9), `UserControllerTest`(9). User CRUD 기준선이므로 이후 도메인은 이 구조를 따른다.

**계층 구조**: `controller` → `service` → `repository` → `domain`. 타입별 패키지 분리.
엔티티는 컨트롤러 밖으로 나가지 않는다 — 요청·응답은 항상 `dto`.

**아직 없는 것** (필요해지면 추가): Spring Security, Redis(`spring-boot-starter-data-redis`), WebSocket, 메시지큐 클라이언트, 마이그레이션 도구(Flyway 등), Docker/K8s 매니페스트.

> **주의**: `ddl-auto: update`는 M1 한정. 컬럼 삭제·타입 변경을 반영하지 못하고 운영에 쓸 수 없다. 스키마가 굳으면 Flyway로 이전할 것.
> Redis 의존성이 아직 없어 **진행 상태·이어풀기는 구현 불가** — 학습 풀이 API 착수 전에 `spring-boot-starter-data-redis`를 추가해야 한다 (4.3이 Redis 전용으로 결정되면서 M1 필수 의존성이 되었다).

---

## 3. 아키텍처 결정

- **모듈러 모놀리스 + 알림 서비스만 별도 프로세스.** MSA 전면 분해는 이 규모에 오버스펙. 알림은 팬아웃 버스트 특성과 결합도가 본체와 명확히 달라서만 분리하고, 메시지큐로 느슨하게 결합한다. K8s에서 각각 Deployment로 두어 독립 스케일링.
- **저장소 역할 분담**
  - **RDBMS** — 정합성 기준(source of truth): 유저, 퀴즈셋/퀴즈, 팔로우, 좋아요, 진행 상태, 학습 기록, 대결 결과
  - **Redis** — 가속 계층·휘발성 상태: 세션/토큰, 좋아요 카운터, 인기 랭킹(ZSET), 학습 진행 세션(TTL), 대결 방 상태·선착순 판정·라이브 스코어보드
  - **메시지큐** — 퀴즈셋 생성 알림 팬아웃, 대결 종료 후처리, (도입 시) RDB↔ES 동기화
- **알림 채널 이원화**: 앱이 켜져 있을 때의 실시간 UI 갱신(대결 스코어보드 등)은 **WebSocket**, 백그라운드/종료 상태에도 도착해야 하는 푸시는 **FCM**. 모바일에서 백그라운드 소켓은 유지되지 않는다.

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

### 4.6 검색
- 퀴즈셋 이름 + 다중 카테고리 복합 검색. 카테고리 다중 선택은 `WHERE category IN (...)` 필터링.
- **서버 필터링(재쿼리) 채택.** 필터 토글 시 클라가 보유한 결과를 거르지 않고 서버에 재요청한다. 페이지네이션 위에서의 클라 필터링은 로드된 부분집합만 걸러 부정확하기 때문.
- **검색 결과에 좋아요 정보 병합**하되 N+1 금지: isLiked는 `WHERE user_id=? AND quizset_id IN (...)` 한 번, 카운트는 Redis `MGET` 한 번.
- ES + Nori는 **속도 때문이 아니라** 형태소 분석·관련도 랭킹·오타 허용·패싯이 필요해질 때 도입. 속도만 필요하면 `pg_trgm`으로 충분. 도입 시 RDB↔ES 동기화는 이벤트/MQ(또는 CDC).

### 4.7 퀴즈셋 CRUD
- 2단계 생성 플로우: ① 이름·설명·카테고리로 퀴즈셋 생성(POST) → ② 수정 페이지에서 퀴즈 추가/삭제/수정.
- 퀴즈 수정은 PUT. 클라이언트는 **변경이 발생했을 때만** 수정 버튼을 활성화해 호출한다.
- 퀴즈 생성 입력: 정답 단어가 포함된 영어 문장 + 정답 단어 + (선택) 힌트.
- **퀴즈셋에는 카테고리가 최소 1개 필요하다** (2026-09-01 결정). 카테고리 없는 퀴즈셋은 §4.6 카테고리 필터 검색에 영원히 잡히지 않아, 만들어지는 순간 발견될 수 없는 데이터가 된다.
  이중으로 막는다 — 사용자 입력은 `QuizSetCreateRequest.categoryIds` 의 `@Size(min = 1)` 이 400 으로 거르고, 그 뒤는 도메인이 스스로 지킨다.
  - `QuizSet` 의 **기본 생성자는 `private`** 이고 **`QuizSet.create(owner, title, description, visibility, categories)` 팩터리만 열려 있다.** 카테고리는 `addCategory` 로 생성 후에 붙는 구조라 생성자 파라미터로는 강제할 수 없어서다.
  - `removeCategory` 는 **마지막 한 개를 지우려 하면 거부**한다.
  - 이 두 곳이 던지는 `IllegalArgumentException` 은 사용자 입력이 아니라 **서버 코드의 실수**를 잡는 방어선이다 (사용자 경로는 이미 400 으로 걸러진 뒤다).

### 4.8 팔로우 + 알림 팬아웃
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
  **코드 체계**: `앞자리 알파벳(도메인) + 3자리 순번`. `C`=Common(횡단), `U`=User, `A`=Auth. 순번은 도메인별 `001`부터, **`999`는 서버 오류·기타로 예약**.

  | enum | code | status |
  |---|---|---|
  | `VALIDATION_FAILED` | `C001` | 400 |
  | `DATA_INTEGRITY_VIOLATION` | `C002` | 409 |
  | `INTERNAL_ERROR` | `C999` | 500 (예약 — 아직 핸들러 없음) |
  | `USER_NOT_FOUND` | `U001` | 404 |
  | `DUPLICATE_EMAIL` | `U002` | 409 |
  | `CATEGORY_NOT_FOUND` | `G001` | 404 |
  | `INVALID_PASSWORD` | `A001` | 400 |

  `G` = cateGory. `C` 는 Common 이 선점해 알파벳이 겹쳐서다.

- **도메인 예외는 `BusinessException(errorCode, detail?)`을 상속**한다. `GlobalExceptionHandler`는 이 타입 하나만 잡으므로 예외를 추가해도 핸들러를 건드릴 일이 없다.
  **클라이언트에는 `ErrorCode.message`(표준 문구)만 나간다.** `detail`(`(id=1)`, `(email=...)` 등 진단용 맥락)은 예외의 `message`에 담겨 **로그로만** 남는다 — 내부 식별자가 응답으로 새지 않게 하기 위해서다.
- **요청 본문을 읽지 못한 경우(`HttpMessageNotReadableException`)도 같은 형식으로 내보낸다** — 깨진 JSON, 필수 필드 누락, 잘못된 enum 값.
  Bean Validation 은 역직렬화가 **성공한 뒤에** 돌기 때문에, Kotlin 논-널 필드가 비면 `@Valid` 가 돌기도 전에 Jackson 이 먼저 실패한다.
  이 핸들러가 없으면 스프링 기본 에러 바디(`timestamp`/`status`/`error`/`path`)가 나가 **클라이언트가 에러 형식 두 개를 파싱**해야 한다. `VALIDATION_FAILED`(C001)로 통일하되, 파서 메시지에는 내부 타입 정보가 섞여 있어 `fieldErrors` 없이 표준 메시지만 내보낸다.
- **페이징 응답은 `PageResponse<T>`로 감싼다.** `Page`(`PageImpl`)를 그대로 직렬화하면 JSON 구조가 Spring Data 내부 구현에 묶여 버전 간 안정성이 깨진다.
- **멱등하지 않은 변경은 PUT이 아니라 POST.** (예: 비밀번호 변경은 현재 비밀번호 대조가 있어 재요청 시 실패 → `POST /{id}/password`)
- **부분 수정은 PATCH.**
- **비밀번호는 어떤 응답에도 담지 않는다.** 해시라도 마찬가지.
- **인증 미도입 상태의 부채**: `{id}` 경로만으로 대상을 지정하므로 현재 누구나 남의 계정을 수정·삭제할 수 있다. 인증 도입 시 소유권 검사를 넣거나 `/api/users/me` 형태로 바꾸고, 유저 전체 목록 조회는 제한/제거한다. (`UserController` KDoc에 명시해 둠)

---

## 5. 테스트 코드 작성 기준

> 판단이 애매하면 **임의로 정하지 말고 작성 전에 질문한다.** 테스트를 과잉 생성하는 것보다 확인하고 진행하는 편이 낫다. 질문이 필요한 상황은 5.6에 정리해 두었다.

### 5.1 테스트 스택
- **JUnit5 + MockK**. 슬라이스 테스트는 `@WebMvcTest`(Controller), 필요 시 `@DataJpaTest`(Repository).
- **Kotest 전환 가능성을 염두에 둔다.** 특정 러너에 강하게 결합된 구조는 피하고, given-when-then의 논리적 구분을 주석이나 구조로 드러내 전환 시 마찰을 줄인다.

- `@WebMvcTest`에서 빈 교체는 **springmockk의 `@MockkBean`**을 쓴다 (부트 기본 `@MockitoBean`은 Mockito 전용). 5.0.1 + 부트 4.1 조합은 `UserControllerTest`로 동작 확인 완료.

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

| 지점 | 문제 | 해법 |
|---|---|---|
| 이메일 중복 가입 | check-then-act 경쟁 | `users.email` **UNIQUE 제약**. `existsByEmail` 선검사는 메시지 품질용일 뿐 보장이 아니다 — `UserService.create`는 `saveAndFlush`로 즉시 INSERT 후 `DataIntegrityViolationException`을 `DuplicateEmailException`으로 변환한다 (`save`만 하면 flush가 커밋 시점으로 밀려 catch 밖에서 터진다) |
| 좋아요 중복 | check-then-act 경쟁 | `(user_id, quizset_id)` **UNIQUE 제약** — DB가 원자적으로 거부 |
| 좋아요 카운트 | 갱신 유실 | Redis 원자 `INCR` + 멱등 flush |
| 대결 선착순 | 최초 정답자 1인 확정 | `winner:{roomId}:{questionId}` 에 `SETNX` — first-writer-wins |
| 대결 방 정원 | 정원 초과 동시 입장 (재고 문제와 동형) | Redis **Lua** 원자 check-and-increment |

---

## 7. 데이터 모델 초안

**RDBMS**
M1 엔티티는 `io.github.ddogga.blanken.domain` 패키지에 **구현 완료**.

```
users                 (id, email, password, nickname, created_at, updated_at)
quiz_set              (id, owner_id, title, description, visibility, like_count, created_at, updated_at)
category              (id, name)                          -- name UNIQUE
quiz_set_category     (quiz_set_id, category_id)          -- 복합 PK, 명시적 조인 엔티티
quiz                  (id, quiz_set_id, sentence, answer_word, hint)
quiz_set_like         (id, user_id, quiz_set_id, created_at, updated_at)  -- UNIQUE(user_id, quiz_set_id)
study_history         (id, user_id, quiz_set_id, score, total_count, correct_count, solved_at)  -- append-only
study_history_detail  (id, history_id, quiz_id, gave_up)

-- 이후 마일스톤
follow                (follower_id, followee_id)
battle_result / battle_result_player
```

**테이블·컬럼 명명 주의**
- `user`, `like`는 PostgreSQL 예약어 → `users`, `quiz_set_like`로 사용한다.
- **`quiz_set_progress` 테이블은 존재하지 않는다.** 진행 상태는 Redis 전용이다 (4.3 참고).

**공통 규약**
- `BaseTimeEntity`(`@MappedSuperclass`) — `created_at` / `updated_at`을 Spring Data JPA Auditing으로 채운다. `@EnableJpaAuditing`은 `config/JpaAuditingConfig.kt`.
- 모든 `@ManyToOne`은 **`FetchType.LAZY` 명시**. 기본값 EAGER는 N+1의 주원인.
- `visibility`는 `@Enumerated(EnumType.STRING)` — ordinal은 enum 순서가 바뀌면 데이터가 깨진다.
- 컬렉션은 내부 `MutableList` + 읽기 전용 `List` 노출 + `addXxx()` 로 양방향 연관 세팅.
- `quiz_set.like_count`는 `protected set` — 정합성 기준은 DB지만 평상시 증감은 Redis이고 이 컬럼은 배치 flush로만 갱신된다.

**Redis 키**
```
session:{token}
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
2. **M2 — 소셜**: 좋아요(Redis 카운터·UNIQUE·DB 정합성) · 검색(서버 필터링·좋아요 병합) · 팔로우 · 알림(MQ + 알림 서비스 + FCM)
3. **M3 — 실시간 대결**: WebSocket · 선착순(SETNX) · 방 정원(Lua) · 스코어보드 · 종료 후처리
4. **M4 — 고도화·운영**: 이어풀기(Redis TTL) · 검색 ES/Nori · Docker · Kubernetes · AWS

**Out of Scope**: 실결제/유료 콘텐츠, 양면시장, AI 문장 자동 생성, 크로스 디바이스 이어풀기.

---

## 9. 미결정 사항

구현에 착수하기 전에 확인이 필요한 것들. 결정되면 이 문서를 갱신할 것.

- **인증 방식** — JWT vs Redis 세션. M1 첫 작업이고 이후 모든 API와 WebSocket 핸드셰이크 인증에 영향. Spring Security 의존성이 아직 없다.
- **퀴즈셋당 평균 문제 수** — 이어풀기와 Redis 도입 정당성에 직결. 10개 남짓이면 이어풀기 가치가 약해지고, 50~100개면 Redis 세션이 확실히 정당화된다.
- **빈 퀴즈셋 처리** — 2단계 생성 플로우의 부작용. draft 상태를 `quiz_set`에 넣을지를 스키마 확정 전에 정해야 한다.
- **Redis 장애 시 진행 상태 정책** — 진행 상태가 Redis 전용이 되면서 Redis 유실 = 진행중 세션 전멸이다. 애초에 24h 안에 사라질 값이라 감수 가능한 손실로 보지만, 사용자 안내 문구는 필요하다.
- **이어풀기 만료 기준** — 24h 롤링 vs 자정 기준 / 이어풀기 시작 시 TTL 갱신 여부 / 만료 안내 문구.
- **학습 기록 상한** — 유저·퀴즈셋당 기록 상한 또는 오래된 기록 요약 정책 (초기엔 무제한 append로 시작 가능). 오답 재풀이 대상 범위(최근 시도의 오답 vs 누적 포기 문제 전체).
- **채점 UX 세부** — 실시간 대조 vs 확인 시 대조, 조기 정답 확정 처리.
- **ES 도입 시점**.
- RDBMS는 PostgreSQL로 사실상 확정(build.gradle + application.yaml). PRD의 "MySQL/PostgreSQL 선택" 이슈는 해소된 것으로 본다.
