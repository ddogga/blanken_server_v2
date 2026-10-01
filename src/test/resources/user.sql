-- ──────────────────────────────────────────────────────────────────────────
-- 유저 50명 = 손으로 쓴 3명 + 생성한 47명
--
-- 동시성 테스트(`QuizSetLikeConcurrentTest`)가 유저 수만큼 스레드를 띄우므로,
-- 여기 인원수가 곧 동시 요청 수가 된다.
-- random() 을 쓰지 않으므로 몇 번을 다시 만들어도 같은 데이터가 나온다.
--
-- 전부 **일반(로컬) 계정**이다 — `provider` / `provider_id` 가 NULL.
-- 비밀번호는 모두 `password123!` 의 BCrypt 해시다 (cost 10).
--
-- `users.email` 의 UNIQUE 가 없어져서(같은 이메일로 소셜·일반 가입 허용)
-- `ON CONFLICT (email)` 을 쓸 수 없다. 남은 UNIQUE 는 `(provider, provider_id)` 뿐인데
-- 로컬 계정은 둘 다 NULL 이고 PostgreSQL 에서 NULL 끼리는 충돌로 잡히지 않는다.
-- 그래서 행마다 `WHERE NOT EXISTS` 로 직접 거른다. (data.sql 과 같은 방식)
-- ──────────────────────────────────────────────────────────────────────────
WITH names(i, name) AS (
    VALUES (0, '지민'), (1, '서연'), (2, '하준'), (3, '도윤'), (4, '서준'),
           (5, '하은'), (6, '예준'), (7, '수아'), (8, '지호'), (9, '윤서')
)
INSERT INTO users (email, password, nickname, provider, provider_id, user_status, user_role, created_at, updated_at)
SELECT d.email,
       '$2y$10$8wQ5/.EDz75v9fNDiPOux.awk0flP4dWpOeZ7.NLGYOBtFFaGHvnK',
       d.nickname,
       NULL,
       NULL,
       'ACTIVE',
       'USER',
       now() - (d.days_ago || ' days')::interval,
       now() - (d.days_ago || ' days')::interval
FROM (
    -- ① 손으로 쓴 3명. data.sql 과 같은 이메일을 쓴다.
    SELECT * FROM (VALUES
        ('blanken@blanken.io', 'blanken', 90),
        ('hyeon@blanken.io',   '현이',    60),
        ('mina@blanken.io',    '미나',    30)
    ) AS curated(email, nickname, days_ago)

    UNION ALL

    -- ② 생성 47명 (4~50번). created_at 을 8~100일 전으로 흩어 둔다.
    SELECT 'user' || g.n || '@blanken.io',
           n.name || g.n,
           g.n * 2
    FROM generate_series(4, 50) AS g(n)
    JOIN names n ON n.i = g.n % 10
) AS d(email, nickname, days_ago)
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = d.email AND u.provider IS NULL
);
