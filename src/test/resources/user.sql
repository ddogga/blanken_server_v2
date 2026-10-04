-- ──────────────────────────────────────────────────────────────────────────
-- 유저 50명 = 손으로 쓴 3명 + 생성한 47명
--
-- 동시성 테스트(`QuizSetLikeConcurrentTest`)가 유저 수만큼 스레드를 띄우므로,
-- 여기 인원수가 곧 동시 요청 수가 된다. 따라서 50명 전부 ACTIVE / USER 여야 한다.
-- random() 을 쓰지 않으므로 몇 번을 다시 만들어도 같은 데이터가 나온다.
--
-- 2026-10-04 User 엔티티 변경 반영
--   - `password` 컬럼이 사라졌다 — 소셜 로그인/가입만 허용하므로 비밀번호를 보관하지 않는다.
--     그래서 50명 전부 **소셜 계정**이다 (`provider` / `provider_id` 가 NOT NULL).
--   - `terms_agreed_at` / `marketing_agreed` / `withdrawn_at` 이 추가됐다.
--     `marketing_agreed` 는 NOT NULL 이라 모든 행이 값을 가져야 한다.
--   - `provider` / `provider_id` 가 NOT NULL 이 되면서 `(provider, provider_id)` UNIQUE 가
--     실제로 동작한다. 예전엔 로컬 계정의 두 컬럼이 NULL 이었고 PostgreSQL 에서 NULL 끼리는
--     충돌로 잡히지 않아 `WHERE NOT EXISTS` 로 우회해야 했는데, 이제 ON CONFLICT 를 그대로 쓴다.
--
-- `provider_id` 는 전부 `seed-` 로 시작한다 (data.sql 과 같은 규칙).
-- 손으로 쓴 3명은 data.sql 과 **같은 (provider, provider_id)** 를 쓰므로,
-- 두 파일이 같은 DB 에 들어가도 ON CONFLICT 가 걸려 중복되지 않는다.
-- ──────────────────────────────────────────────────────────────────────────
WITH names(i, name) AS (
    VALUES (0, '지민'), (1, '서연'), (2, '하준'), (3, '도윤'), (4, '서준'),
           (5, '하은'), (6, '예준'), (7, '수아'), (8, '지호'), (9, '윤서')
)
INSERT INTO users (email, nickname, provider, provider_id,
                   user_status, user_role,
                   terms_agreed_at, marketing_agreed, withdrawn_at,
                   created_at, updated_at)
SELECT d.email,
       d.nickname,
       d.provider,
       d.provider_id,
       'ACTIVE',
       'USER',
       -- 전원 가입을 완료한 계정이므로 약관 동의 시각이 있다. 탈퇴자는 없다.
       now() - (d.days_ago || ' days')::interval,
       d.marketing_agreed,
       NULL::timestamptz,
       now() - (d.days_ago || ' days')::interval,
       now() - (d.days_ago || ' days')::interval
FROM (
    -- ① 손으로 쓴 3명. data.sql 과 같은 이메일·제공자·provider_id 를 쓴다.
    SELECT * FROM (VALUES
        ('blanken@blanken.io', 'blanken', 'KAKAO', 'seed-blanken', true,  90),
        ('hyeon@blanken.io',   '현이',    'KAKAO', 'seed-hyeon',   false, 60),
        ('mina@blanken.io',    '미나',    'NAVER', 'seed-mina',    true,  30)
    ) AS curated(email, nickname, provider, provider_id, marketing_agreed, days_ago)

    UNION ALL

    -- ② 생성 47명 (4~50번). created_at 을 8~100일 전으로 흩어 둔다.
    --    제공자는 번갈아 가며, 마케팅 수신 동의는 3명 중 1명꼴로 준다.
    SELECT 'user' || g.n || '@blanken.io',
           n.name || g.n,
           CASE WHEN g.n % 2 = 0 THEN 'KAKAO' ELSE 'NAVER' END,
           'seed-user' || g.n,
           (g.n % 3 = 0),
           g.n * 2
    FROM generate_series(4, 50) AS g(n)
    JOIN names n ON n.i = g.n % 10
) AS d(email, nickname, provider, provider_id, marketing_agreed, days_ago)
ON CONFLICT (provider, provider_id) DO NOTHING;
