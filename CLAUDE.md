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

거의 뼈대만 있는 초기 상태 (M1 시작 전).

| 항목 | 값 |
|---|---|
| 언어/런타임 | Kotlin 2.3.21, Java 21 toolchain |
| 프레임워크 | Spring Boot 4.1.0 (`spring-boot-starter-webmvc`, `-data-jpa`) |
| DB | PostgreSQL (`ddl-auto: update`, `open-in-view: false`) |
| API 문서 | springdoc-openapi 3.1.0 — `/swagger-ui.html` |
| 패키지 루트 | `io.github.ddogga.blanken` |

기존 코드: `BlankenApplication.kt`, `config/OpenApiConfig.kt`, `controller/TestController.kt` 뿐.

**아직 없는 것** (필요해지면 추가): Spring Security, Redis(`spring-boot-starter-data-redis`), WebSocket, 메시지큐 클라이언트, 마이그레이션 도구(Flyway 등), Docker/K8s 매니페스트.

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

### 4.3 학습 히스토리는 2테이블로 분리
- `quiz_set_progress` — 유저-퀴즈셋당 **1행, 가변**. `status`(IN_PROGRESS / COMPLETED). "진행중/완료 목록"은 같은 테이블을 status로 필터.
- `study_history` — 완주할 때마다 **append-only, 불변**. 점수·오답 목록·완료 시각.
  최근 1건 덮어쓰기가 아니라 **누적**하는 이유: 성장 추이·오답 변화 추적이 학습 앱의 핵심 가치이고, 불변 이벤트 누적이 갱신 충돌 없이 통계에 유리하기 때문.

### 4.4 이어풀기 — TTL이 곧 정책
- 중단한 학습 세션은 **하루 이내에만** 이어풀기 가능. 지나면 처음부터.
- 근거는 자원 절약이 아니라 **학습적 판단**: 오래 방치한 뒤의 이어풀기는 복습 효과가 낮다.
- 구현: 진행 세션을 Redis TTL 24h로 저장 → 별도 만료 정리 로직 불필요, 신뢰할 수 없는 "이탈 감지" 이벤트에 의존하지 않음.
- **완주 시에만 RDB flush**(멱등). 미완료 세션은 TTL로 자연 소멸.

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

### 4.8 팔로우 + 알림 팬아웃
- 생성 API는 `QuizSetCreated` 이벤트만 발행하고 **즉시 응답**. 알림 서비스 컨슈머가 팔로워 목록을 조회해 알림 생성/전송, 실패 시 재시도(DLQ).
- 절대 생성 API의 트랜잭션 안에서 팔로워 전체에 동기 전송하지 않는다.

### 4.9 실시간 대결 (선착순)
- 한 퀴즈셋으로 여러 참가자가 문제를 순차 진행, 각 문제를 **가장 먼저 맞힌 한 명**이 득점.
- WebSocket 메시지: `QUESTION_START` / `SUBMIT_ANSWER` / `ROUND_RESULT` / `SCOREBOARD_UPDATE` / `GAME_OVER`. 방 상태는 Redis에 TTL 저장, **서버 주도 타이머**로 진행.
- 판정 기준은 **서버 도착 순서** — 클라이언트 타임스탬프는 믿지 않는다.
- 종료 후처리는 `BattleFinished` 이벤트로 큐에 넘겨 진행 프로세스의 응답성을 유지한다.

---

## 5. 동시성 제어 — 이 프로젝트의 핵심 축

> **Redis 싱글스레드는 "명령 하나의 원자성"만 보장한다. 여러 명령에 걸친 논리적 원자성은 UNIQUE 제약·SETNX·Lua 스크립트로 별도 설계한다.**

| 지점 | 문제 | 해법 |
|---|---|---|
| 좋아요 중복 | check-then-act 경쟁 | `(user_id, quizset_id)` **UNIQUE 제약** — DB가 원자적으로 거부 |
| 좋아요 카운트 | 갱신 유실 | Redis 원자 `INCR` + 멱등 flush |
| 대결 선착순 | 최초 정답자 1인 확정 | `winner:{roomId}:{questionId}` 에 `SETNX` — first-writer-wins |
| 대결 방 정원 | 정원 초과 동시 입장 (재고 문제와 동형) | Redis **Lua** 원자 check-and-increment |

---

## 6. 데이터 모델 초안

**RDBMS**
```
user                  (id, email, password_hash, nickname, created_at)
follow                (follower_id, followee_id)
quiz_set              (id, owner_id, title, description, visibility, like_count, created_at)
quiz_set_category     (quiz_set_id, category_id)          -- 다중 카테고리 매핑
quiz                  (id, quiz_set_id, sentence, answer_word, hint)
like                  (user_id, quiz_set_id)              -- UNIQUE(user_id, quiz_set_id)
quiz_set_progress     (user_id, quiz_set_id, status, ...) -- 유저-퀴즈셋당 1행
study_history         (id, user_id, quiz_set_id, score, solved_at)  -- append-only
study_history_detail  (id, history_id, quiz_id, gave_up)
battle_result / battle_result_player
```
※ `like`는 PostgreSQL 예약어이므로 테이블명 인용/변경 필요.

**Redis 키**
```
session:{token}
like:count:{quizSetId}
ranking:quizset                          (ZSET)
study:session:{userId}:{quizSetId}       (Hash, TTL 24h)  -- 이어풀기
battle:room:{roomId}
battle:score:{roomId}                    (ZSET)
winner:{roomId}:{questionId}             (SETNX)
battle:room:{roomId}:count
```

**메시지큐 이벤트**: `QuizSetCreated`(알림 팬아웃), `BattleFinished`(히스토리·알림), (ES 도입 시) `QuizSetChanged`

---

## 7. 마일스톤

1. **M1 — 기반**: 인증 · Quiz Set CRUD · 학습 풀이(클라 채점·재시도/포기·오답 재풀이) · 히스토리 2테이블
2. **M2 — 소셜**: 좋아요(Redis 카운터·UNIQUE·DB 정합성) · 검색(서버 필터링·좋아요 병합) · 팔로우 · 알림(MQ + 알림 서비스 + FCM)
3. **M3 — 실시간 대결**: WebSocket · 선착순(SETNX) · 방 정원(Lua) · 스코어보드 · 종료 후처리
4. **M4 — 고도화·운영**: 이어풀기(Redis TTL) · 검색 ES/Nori · Docker · Kubernetes · AWS

**Out of Scope**: 실결제/유료 콘텐츠, 양면시장, AI 문장 자동 생성, 크로스 디바이스 이어풀기.

---

## 8. 미결정 사항

구현에 착수하기 전에 확인이 필요한 것들. 결정되면 이 문서를 갱신할 것.

- **인증 방식** — JWT vs Redis 세션. M1 첫 작업이고 이후 모든 API와 WebSocket 핸드셰이크 인증에 영향. Spring Security 의존성이 아직 없다.
- **퀴즈셋당 평균 문제 수** — 이어풀기와 Redis 도입 정당성에 직결. 10개 남짓이면 이어풀기 가치가 약해지고, 50~100개면 Redis 세션이 확실히 정당화된다.
- **빈 퀴즈셋 처리** — 2단계 생성 플로우의 부작용. draft 상태를 `quiz_set`에 넣을지를 스키마 확정 전에 정해야 한다.
- **이어풀기 만료 기준** — 24h 롤링 vs 자정 기준 / 이어풀기 시작 시 TTL 갱신 여부 / 만료 안내 문구.
- **학습 기록 상한** — 유저·퀴즈셋당 기록 상한 또는 오래된 기록 요약 정책 (초기엔 무제한 append로 시작 가능). 오답 재풀이 대상 범위(최근 시도의 오답 vs 누적 포기 문제 전체).
- **채점 UX 세부** — 실시간 대조 vs 확인 시 대조, 조기 정답 확정 처리.
- **ES 도입 시점**.
- RDBMS는 PostgreSQL로 사실상 확정(build.gradle + application.yaml). PRD의 "MySQL/PostgreSQL 선택" 이슈는 해소된 것으로 본다.
