-- 개발용 초기 데이터.
--
-- 여러 번 실행해도 안전하다.
--   카테고리 — `name` UNIQUE 를 이용한 ON CONFLICT DO NOTHING
--   유저     — `(provider, provider_id)` UNIQUE 를 이용한 ON CONFLICT DO NOTHING
--   퀴즈셋·퀴즈 — 유니크 제약이 없어 `WHERE NOT EXISTS` 로 **테이블이 비어 있을 때만** 넣는다.
--                 (퀴즈셋이 한 건이라도 있으면 그 블록은 통째로 건너뛴다.)
--
-- 2026-10-04 User 엔티티 변경 반영
--   - `password` 컬럼이 사라졌다 — 소셜 로그인/가입만 허용하므로 비밀번호를 보관하지 않는다.
--     그래서 시드 유저도 전부 **소셜 계정**이다 (`provider` / `provider_id` 가 NOT NULL).
--   - `terms_agreed_at` / `marketing_agreed` / `withdrawn_at` 이 추가됐다.
--     `marketing_agreed` 는 NOT NULL 이라 모든 행이 값을 가져야 한다.
--
-- 수동 실행:  psql -U user -d blanken -f src/main/resources/data.sql
-- 기동 시 자동 실행:  application.yaml 에 아래 두 줄 추가
--   spring.sql.init.mode: always
--   spring.jpa.defer-datasource-initialization: true   (Hibernate 가 테이블을 만든 뒤 실행되도록)


-- ──────────────────────────────────────────────────────────────────────────
-- 카테고리 10개
-- ──────────────────────────────────────────────────────────────────────────
INSERT INTO category (name) VALUES
    ('토익'),
    ('토플'),
    ('수능'),
    ('비즈니스'),
    ('일상회화'),
    ('여행'),
    ('문법'),
    ('숙어'),
    ('동사'),
    ('시사')
ON CONFLICT (name) DO NOTHING;


-- ──────────────────────────────────────────────────────────────────────────
-- 유저 6명 — 전부 **소셜 계정**이다. 로컬(비밀번호) 계정은 더 이상 존재하지 않는다.
--
-- 앞의 4명은 기존과 같은 이메일·닉네임을 유지한다.
--   - 앞 3명은 아래 퀴즈셋 블록이 이메일로 직접 참조하므로 이름·순서를 바꾸지 않는다.
--   - `test@test.io` 는 퀴즈셋을 하나도 갖지 않는 **소비자 유저**다.
--     파일 맨 아래 좋아요 블록이 이 유저 앞으로 30건을 달아 둔다.
--     `QuizSetLikeRepositoryTest` 가 `findByEmail` 로 이 유저를 찾으므로
--     **이 이메일을 가진 행은 반드시 하나뿐이어야 한다.**
--
-- 뒤의 2명은 새로 추가된 상태 전이를 눈으로 확인하기 위한 것이다.
--   - PENDING/GUEST  : 소셜 인증만 끝나고 추가 정보를 아직 안 넣은 상태.
--                      `UserService.socialRegister` 가 만드는 모습 그대로다
--                      (이메일·닉네임이 NEW_USER_EMAIL / NEW_USER_NAME 과 같다).
--   - WITHDRAWN/USER : 탈퇴 후 재가입 쿨타임 중인 계정. `withdrawn_at` 이 채워져 있다.
--
-- `provider_id` 는 전부 `seed-` 로 시작한다. 실제 소셜 로그인으로 생긴 계정과
-- 섞이지 않게 아래 블록들이 이 접두사로 시드 유저만 골라낸다.
--
-- `users.email` 에는 UNIQUE 가 없지만(같은 이메일로 제공자별 1개씩 가입 가능)
-- `(provider, provider_id)` 에는 UNIQUE 가 있고 시드 유저는 둘 다 NOT NULL 이라
-- 예전처럼 `WHERE NOT EXISTS` 로 우회할 필요 없이 ON CONFLICT 를 그대로 쓸 수 있다.
-- ──────────────────────────────────────────────────────────────────────────
INSERT INTO users (email, nickname, provider, provider_id,
                   user_status, user_role,
                   terms_agreed_at, marketing_agreed, withdrawn_at,
                   created_at, updated_at)
SELECT d.email,
       d.nickname,
       d.provider,
       d.provider_id,
       d.user_status,
       d.user_role,
       -- 약관 동의 시각은 가입을 **완료**한 계정만 갖는다 (`User.completeSignup` 에서 채워진다).
       CASE WHEN d.user_status = 'PENDING' THEN NULL
            ELSE now() - (d.days_ago || ' days')::interval END,
       d.marketing_agreed,
       -- 탈퇴 시각은 WITHDRAWN 만 갖는다 (`User.deleteUser` 에서 채워진다).
       CASE WHEN d.user_status = 'WITHDRAWN' THEN now() - interval '12 hours'
            ELSE NULL END,
       now() - (d.days_ago || ' days')::interval,
       now() - (d.days_ago || ' days')::interval
FROM (VALUES
    ('blanken@blanken.io', 'blanken',     'KAKAO', 'seed-blanken',   'ACTIVE',    'USER',  true,  90),
    ('hyeon@blanken.io',   '현이',        'KAKAO', 'seed-hyeon',     'ACTIVE',    'USER',  false, 60),
    ('mina@blanken.io',    '미나',        'NAVER', 'seed-mina',      'ACTIVE',    'USER',  true,  30),
    ('test@test.io',       '테스터',      'KAKAO', 'seed-test',      'ACTIVE',    'USER',  false, 10),
    ('newbie@blanken.com', '새로운 유저', 'NAVER', 'seed-pending',   'PENDING',   'GUEST', false,  2),
    ('bye@blanken.io',     '떠난이',      'KAKAO', 'seed-withdrawn', 'WITHDRAWN', 'USER',  false, 20)
) AS d(email, nickname, provider, provider_id, user_status, user_role, marketing_agreed, days_ago)
ON CONFLICT (provider, provider_id) DO NOTHING;


-- ──────────────────────────────────────────────────────────────────────────
-- 퀴즈셋 100개 = 손으로 쓴 20개 + 생성한 80개
--
-- 앞의 20개는 제목·설명이 실제 서비스처럼 읽히도록 직접 썼다. 목록 화면을 눈으로 확인할 때 쓴다.
-- 뒤의 80개는 페이징(20개씩 5페이지)과 정렬을 시험할 부피용이다.
--
-- - 유저 3명 / 카테고리 10개에 고루 분산
-- - `quiz_count` 는 5~15 사이. 아래 퀴즈 블록이 이 값만큼 생성하므로 실제 개수와 항상 일치한다.
-- - `created_at` 을 흩어 둔다 — 최신순(CREATE_AT_DESC) 정렬 확인용.
-- - `like_count` 를 흩어 두되 **일부러 같은 값이 많이 나오게** 한다(0/10/20/30/40).
--   동점일 때 `id DESC` 타이브레이커가 없으면 페이지 사이에서 항목이 중복·누락되는데,
--   전부 다른 값이면 그 버그가 드러나지 않는다. `created_at` 도 같은 이유로 겹치게 둔다.
-- - PRIVATE 을 섞어 둔다. 검색이 공개 퀴즈셋만 노출하는지 확인하는 용도다.
-- ──────────────────────────────────────────────────────────────────────────
WITH owners(i, email) AS (
    VALUES (0, 'blanken@blanken.io'), (1, 'hyeon@blanken.io'), (2, 'mina@blanken.io')
),
cats(i, name) AS (
    VALUES (0, '토익'), (1, '토플'), (2, '수능'), (3, '비즈니스'), (4, '일상회화'),
           (5, '여행'), (6, '문법'), (7, '숙어'), (8, '동사'), (9, '시사')
),
kinds(i, label) AS (
    VALUES (0, '기본편'), (1, '심화편'), (2, '실전편'), (3, '복습'), (4, '정리')
)
INSERT INTO quiz_set (owner_id, category_id, title, description, visibility, like_count, quiz_count, created_at, updated_at)
SELECT u.id,
       c.id,
       d.title,
       d.description,
       d.visibility,
       d.like_count,
       d.quiz_count,
       now() - (d.days_ago || ' days')::interval,
       now() - (d.days_ago || ' days')::interval
FROM (
    -- ① 손으로 쓴 20개
    SELECT * FROM (VALUES
    ('blanken@blanken.io', '토익',     '토익 빈출 동사 30선',      '파트5에서 반복되는 동사만 모았다',        'PUBLIC',  142,  5, 80),
    ('blanken@blanken.io', '토익',     '토익 파트5 문법 포인트',   '시간 없을 때 이것만',                     'PUBLIC',   98,  6, 76),
    ('blanken@blanken.io', '비즈니스', '토익 비즈니스 이메일 표현','메일에서 자주 나오는 정형 표현',          'PUBLIC',   71,  7, 72),
    ('hyeon@blanken.io',   '토플',     '토플 리딩 핵심 어휘',      '리딩 지문에서 반복되는 학술 어휘',        'PUBLIC',   64,  8, 68),
    ('hyeon@blanken.io',   '토플',     '토플 라이팅 연결어',       '에세이 흐름을 만드는 연결어',             'PUBLIC',   55,  9, 64),
    ('mina@blanken.io',    '수능',     '수능 영어 빈칸 추론',      '빈칸 유형에 자주 나오는 어휘',            'PUBLIC',   88, 10, 60),
    ('mina@blanken.io',    '숙어',     '수능 필수 숙어',           '기출에서 뽑은 숙어 모음',                 'PUBLIC',   47, 11, 56),
    ('blanken@blanken.io', '비즈니스', '회의에서 쓰는 표현',       '영어 회의 진행에 필요한 문장',            'PUBLIC',   33, 12, 52),
    ('hyeon@blanken.io',   '비즈니스', '이메일 정중한 요청',       '무례하지 않게 부탁하는 법',               'PUBLIC',   29, 13, 48),
    ('mina@blanken.io',    '일상회화', '일상 회화 기초 동사',      '매일 쓰는 동사부터',                      'PUBLIC',  120, 14, 44),
    ('blanken@blanken.io', '일상회화', '카페에서 주문하기',        '주문할 때 막히지 않기',                   'PUBLIC',   76, 15, 40),
    ('hyeon@blanken.io',   '여행',     '공항에서 쓰는 영어',       '체크인부터 보안검색까지',                 'PUBLIC',   91,  5, 36),
    ('mina@blanken.io',    '여행',     '호텔 체크인 표현',         '숙소에서 필요한 문장',                    'PUBLIC',   38,  6, 32),
    ('blanken@blanken.io', '여행',     '길 묻고 답하기',           '방향 표현 정리',                          'PUBLIC',   25,  7, 28),
    ('hyeon@blanken.io',   '문법',     '가정법 완전 정복',         '가정법 과거부터 혼합까지',                'PUBLIC',  105,  8, 24),
    ('mina@blanken.io',    '문법',     '관계대명사 정리',          'who, which, that 구분',                   'PUBLIC',   59,  9, 20),
    ('blanken@blanken.io', '문법',     '자주 틀리는 전치사',       'in, on, at 헷갈림 해소',                  'PUBLIC',   82, 10, 16),
    ('hyeon@blanken.io',   '숙어',     '헷갈리는 구동사',          'take, get, put 조합',                     'PUBLIC',   44, 11, 12),
    ('mina@blanken.io',    '시사',     '뉴스 헤드라인 어휘',       '기사 제목에 자주 나오는 단어',            'PUBLIC',   17, 12,  8),
    ('blanken@blanken.io', '동사',     '나만 보는 오답 노트',      '공개하지 않는 개인용',                    'PRIVATE',   0, 13,  4)
    ) AS curated(email, category, title, description, visibility, like_count, quiz_count, days_ago)

    UNION ALL

    -- ② 생성 80개 (21~100번)
    --    n 을 나눈 나머지로 소유자·카테고리·제목을 결정한다. random() 을 쓰지 않으므로
    --    몇 번을 다시 만들어도 같은 데이터가 나온다.
    SELECT o.email,
           c.name,
           c.name || ' ' || k.label || ' ' || g.n,
           c.name || ' ' || k.label || ' 연습 문제 모음',
           CASE WHEN g.n % 13 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END,
           (g.n % 5) * 10,        -- 0/10/20/30/40 — 동점을 많이 만든다
           5 + (g.n % 11),        -- 5~15
           g.n % 50               -- created_at 도 겹치게
    FROM generate_series(21, 100) AS g(n)
    JOIN owners o ON o.i = g.n % 3
    JOIN cats   c ON c.i = g.n % 10
    JOIN kinds  k ON k.i = g.n % 5
) AS d(email, category, title, description, visibility, like_count, quiz_count, days_ago)
-- 이메일이 UNIQUE 가 아니므로 소유자를 **시드 계정으로 한정**한다.
-- 같은 이메일로 실제 소셜 가입이 생기면 조인이 두 배로 불어난다.
JOIN users u ON u.email = d.email AND u.provider_id LIKE 'seed-%'
JOIN category c ON c.name = d.category
WHERE NOT EXISTS (SELECT 1 FROM quiz_set);


-- ──────────────────────────────────────────────────────────────────────────
-- 퀴즈 — 퀴즈셋마다 `quiz_count` 개 (5~15)
--
-- 문장 풀 16개를 퀴즈셋마다 시작 위치를 바꿔 가며 돌려 쓴다.
-- 풀 크기(16) > 최대 quiz_count(15) 라 한 퀴즈셋 안에서 문장이 겹치지 않는다.
--
-- 모든 문장은 `Quiz.SENTENCE_PATTERN` 을 지킨다 — 빈칸 {{}} 이 정확히 하나, 그 외 중괄호 없음.
-- ──────────────────────────────────────────────────────────────────────────
WITH pool(n, sentence, answer_word, hint) AS (
    VALUES
        ( 1, 'She decided to {{}} the meeting until next week.',          'postpone',  '미루다, 연기하다'),
        ( 2, 'Please {{}} the attached document before Friday.',          'review',    '검토하다'),
        ( 3, 'We need to {{}} the budget for the next quarter.',          'allocate',  '배정하다, 할당하다'),
        ( 4, 'The manager will {{}} the new policy tomorrow.',            'announce',  '발표하다'),
        ( 5, 'Could you {{}} me the sales report by noon?',               'send',      '보내다'),
        ( 6, 'They failed to {{}} the deadline despite working late.',    'meet',      '(기한을) 맞추다'),
        ( 7, 'He was asked to {{}} his opinion during the discussion.',   'express',   '표현하다'),
        ( 8, 'The company plans to {{}} its business into Asia.',         'expand',    '확장하다'),
        ( 9, 'I would like to {{}} a room for two nights.',               'reserve',   '예약하다'),
        (10, 'Make sure to {{}} your passport at the counter.',           'present',   '제시하다'),
        (11, 'The flight was {{}} due to bad weather.',                   'delayed',   '지연된'),
        (12, 'Can you {{}} me how to get to the station?',                'tell',      '알려주다'),
        (13, 'She managed to {{}} the problem on her own.',               'solve',     '해결하다'),
        (14, 'We should {{}} this issue at the next meeting.',            'discuss',   '논의하다'),
        (15, 'The report needs to be {{}} by the end of the day.',        'submitted', '제출된'),
        (16, 'He decided to {{}} up early to catch the first train.',     'wake',      '일어나다')
),
numbered AS (
    SELECT id,
           quiz_count,
           (row_number() OVER (ORDER BY id) - 1)::int AS idx
    FROM quiz_set
)
INSERT INTO quiz (quiz_set_id, sentence, answers, hint, order_index, created_at, updated_at)
SELECT s.id, p.sentence, ARRAY[p.answer_word], p.hint, g.n - 1, now(), now()
FROM numbered s
CROSS JOIN LATERAL generate_series(1, s.quiz_count) AS g(n)
JOIN pool p ON p.n = ((s.idx + g.n - 1) % 16) + 1
WHERE NOT EXISTS (SELECT 1 FROM quiz);


-- ──────────────────────────────────────────────────────────────────────────
-- 좋아요 30개 — 전부 `test@test.io` 가 누른 것
--
-- "내가 좋아요한 퀴즈셋 목록" 을 확인하기 위한 데이터다.
-- 20개씩 끊으면 2페이지(20 + 10)가 나오도록 30개를 넣는다.
--
-- 대상은 **공개 퀴즈셋만** id 순으로 앞에서 30개. random() 을 쓰지 않으므로 재현 가능하다.
-- `created_at` 을 1시간씩 어긋나게 둔다 — 최근에 누른 순 정렬 확인용.
--
-- UNIQUE(user_id, quiz_set_id) 를 이용한 ON CONFLICT DO NOTHING 이라
-- 위 블록들과 달리 "테이블이 비어 있을 때만" 조건 없이도 여러 번 실행할 수 있다.
--
-- 주의: `quiz_set.like_count` 는 **일부러 건드리지 않았다.**
--       위 퀴즈셋 블록이 동점 타이브레이커를 시험하려고 like_count 를 의도적으로
--       겹쳐 놨는데(0/10/20/30/40), 여기서 30개를 +1 하면 그 동점이 깨진다.
--       그래서 좋아요 행 수와 like_count 는 서로 맞지 않는다.
-- ──────────────────────────────────────────────────────────────────────────
INSERT INTO quiz_set_like (user_id, quiz_set_id, created_at, updated_at)
SELECT u.id,
       t.id,
       now() - (t.rn || ' hours')::interval,
       now() - (t.rn || ' hours')::interval
FROM users u
CROSS JOIN (
    SELECT qs.id,
           row_number() OVER (ORDER BY qs.id) AS rn
    FROM quiz_set qs
    WHERE qs.visibility = 'PUBLIC'
    ORDER BY qs.id
    LIMIT 30
) AS t
-- 퀴즈셋 블록과 같은 이유로 시드 계정으로 한정한다.
WHERE u.provider = 'KAKAO' AND u.provider_id = 'seed-test'
ON CONFLICT (user_id, quiz_set_id) DO NOTHING;


-- ──────────────────────────────────────────────────────────────────────────
-- 팔로우 대상 유저 25명 (`follow1@blanken.io` ~ `follow25@blanken.io`)
--
-- 퀴즈셋을 하나도 갖지 않는 **팔로우 대상 전용** 유저들이다.
-- 위쪽 퀴즈셋 블록은 소유자를 이메일로 찾으므로 여기 유저들과는 엮이지 않는다.
--
-- 22번은 PENDING, 23번은 WITHDRAWN 이다 — 나머지는 ACTIVE.
-- `FollowCustomRepositoryImpl.getFollows` 가 `user.userStatus.eq(ACTIVE)` 로 거르는지
-- 확인하기 위한 것이다. 둘 다 팔로우는 하지만 **조회 결과에는 나오지 않아야 한다.**
--
-- 24~25번은 팔로우하지 않는다 — 쿼리가 "전체 유저"를 긁어오지 않는지 보기 위한 대조군이다.
--
-- random() 을 쓰지 않으므로 몇 번을 다시 만들어도 같은 데이터가 나온다.
-- ──────────────────────────────────────────────────────────────────────────
WITH names(i, name) AS (
    VALUES (0, '지민'), (1, '서연'), (2, '하준'), (3, '도윤'), (4, '서준'),
           (5, '하은'), (6, '예준'), (7, '수아'), (8, '지호'), (9, '윤서')
)
INSERT INTO users (email, nickname, provider, provider_id,
                   user_status, user_role,
                   terms_agreed_at, marketing_agreed, withdrawn_at,
                   created_at, updated_at)
SELECT 'follow' || g.n || '@blanken.io',
       n.name || g.n,
       CASE WHEN g.n % 2 = 0 THEN 'KAKAO' ELSE 'NAVER' END,
       'seed-follow' || g.n,
       CASE WHEN g.n = 22 THEN 'PENDING'
            WHEN g.n = 23 THEN 'WITHDRAWN'
            ELSE 'ACTIVE' END,
       CASE WHEN g.n = 22 THEN 'GUEST' ELSE 'USER' END,
       -- 가입을 완료하지 않은 PENDING 만 약관 동의 시각이 없다.
       CASE WHEN g.n = 22 THEN NULL
            ELSE now() - (g.n || ' days')::interval END,
       (g.n % 3 = 0),
       CASE WHEN g.n = 23 THEN now() - interval '6 hours' ELSE NULL END,
       now() - (g.n || ' days')::interval,
       now() - (g.n || ' days')::interval
FROM generate_series(1, 25) AS g(n)
JOIN names n ON n.i = g.n % 10
ON CONFLICT (provider, provider_id) DO NOTHING;


-- ──────────────────────────────────────────────────────────────────────────
-- 팔로우 23건 — 전부 `test@test.io` 가 건 것
--
-- 조회 결과는 **21건**이다. 23건 중 PENDING(22번)·WITHDRAWN(23번) 두 명이
-- `getFollows` 의 `userStatus = ACTIVE` 조건에서 걸러지기 때문이다.
-- 테이블 행 수(23)와 조회 결과(21)가 다른 것은 **의도된 것**이다.
--
-- `getFollows` 는 `follow.id DESC` + `follow.id < cursor` 커서 페이징이다.
-- 21건이면 size=10 으로 10 / 10 / 1 세 페이지가 나와서 마지막 페이지 처리까지 확인된다.
-- id 순서가 곧 정렬 순서이므로 `ORDER BY` 로 삽입 순서를 고정한다.
--
-- 알림 플래그는 일부만 꺼 둔다 — §4.8 알림 팬아웃이 수신 거부를 걸러내는지 확인용.
--   notify_quiz_set_created   : 4명 중 1명 꺼짐
--   notify_quiz_battle_opened : 5명 중 1명 꺼짐
--
-- UNIQUE(follower_id, followee_id) 를 이용한 ON CONFLICT DO NOTHING 이라
-- 여러 번 실행해도 안전하다.
-- ──────────────────────────────────────────────────────────────────────────
INSERT INTO follow (follower_id, followee_id,
                    notify_quiz_set_created, notify_quiz_battle_opened,
                    created_at, updated_at)
SELECT f.id,
       t.id,
       (t.n % 4 <> 0),
       (t.n % 5 <> 0),
       now() - (t.n || ' hours')::interval,
       now() - (t.n || ' hours')::interval
FROM users f
CROSS JOIN (
    SELECT u.id, g.n
    FROM generate_series(1, 23) AS g(n)
    JOIN users u ON u.provider_id = 'seed-follow' || g.n
) AS t
-- 팔로워는 좋아요 블록과 같은 소비자 유저다.
WHERE f.provider = 'KAKAO' AND f.provider_id = 'seed-test'
ORDER BY t.n
ON CONFLICT (follower_id, followee_id) DO NOTHING;


-- ──────────────────────────────────────────────────────────────────────────
-- 팔로워 23건 — 전부 `blanken@blanken.io` 를 팔로우한 것 (`getFollowers` 확인용)
--
-- 위 팔로우 블록과 **방향이 반대**다. 그쪽은 `test@test.io` 가 23명을 팔로우하고,
-- 여기는 23명(`follow1` ~ `follow23`)이 `blanken@blanken.io` 를 팔로우한다.
-- 대상 유저를 `test@test.io` 와 분리해 둔 이유: 한 유저에 팔로잉·팔로워가 섞이면
-- `follower_id` / `followee_id` 를 뒤바꿔 조회하는 실수가 결과 수로 드러나지 않는다.
--
-- 조회 결과는 **21건**이다. PENDING(22번)·WITHDRAWN(23번) 두 명이
-- `getFollowers` 의 `userStatus = ACTIVE` 조건에서 걸러진다. 24~25번은 대조군이라 팔로우하지 않는다.
-- size=10 이면 10 / 10 / 1 세 페이지.
--
-- UNIQUE(follower_id, followee_id) 를 이용한 ON CONFLICT DO NOTHING 이라
-- 여러 번 실행해도 안전하다.
-- ──────────────────────────────────────────────────────────────────────────
INSERT INTO follow (follower_id, followee_id,
                    notify_quiz_set_created, notify_quiz_battle_opened,
                    created_at, updated_at)
SELECT f.id,
       t.id,
       (f.n % 4 <> 0),
       (f.n % 5 <> 0),
       now() - (f.n || ' minutes')::interval,
       now() - (f.n || ' minutes')::interval
FROM (
    SELECT u.id, g.n
    FROM generate_series(1, 23) AS g(n)
    JOIN users u ON u.provider_id = 'seed-follow' || g.n
) AS f
CROSS JOIN users t
WHERE t.provider = 'KAKAO' AND t.provider_id = 'seed-blanken'
ORDER BY f.n
ON CONFLICT (follower_id, followee_id) DO NOTHING;
