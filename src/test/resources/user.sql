-- ──────────────────────────────────────────────────────────────────────────
-- 유저 50명 = 손으로 쓴 3명 + 생성한 47명
--
-- 앞의 3명은 아래 퀴즈셋 블록이 이메일로 직접 참조하므로 이름·순서를 바꾸지 않는다.
-- 뒤의 47명은 목록·페이징처럼 부피가 필요한 화면을 확인하기 위한 것이다.
-- random() 을 쓰지 않으므로 몇 번을 다시 만들어도 같은 데이터가 나온다.
--
-- 비밀번호는 모두 `password123!` 의 BCrypt 해시다 (cost 10).
-- 인증 도입 전이라 당장 쓰이지 않지만, 나중에 그대로 로그인되도록 실제 해시를 넣어 둔다.
-- ──────────────────────────────────────────────────────────────────────────
WITH names(i, name) AS (
    VALUES (0, '지민'), (1, '서연'), (2, '하준'), (3, '도윤'), (4, '서준'),
           (5, '하은'), (6, '예준'), (7, '수아'), (8, '지호'), (9, '윤서')
)
INSERT INTO users (email, password, nickname, created_at, updated_at)
SELECT d.email,
       '$2y$10$8wQ5/.EDz75v9fNDiPOux.awk0flP4dWpOeZ7.NLGYOBtFFaGHvnK',
       d.nickname,
       now() - (d.days_ago || ' days')::interval,
       now() - (d.days_ago || ' days')::interval
FROM (
    -- ① 손으로 쓴 3명 — 아래 퀴즈셋 블록이 이 이메일을 참조한다
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
ON CONFLICT (email) DO NOTHING;