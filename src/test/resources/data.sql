-- 개발용 초기 데이터.
--
-- 여러 번 실행해도 안전하다. 카테고리는 name UNIQUE 를 이용한 ON CONFLICT DO NOTHING,
-- 유저는 email UNIQUE 를 이용한 ON CONFLICT DO NOTHING,
-- 퀴즈셋·퀴즈는 유니크 제약이 없어 `WHERE NOT EXISTS` 로 **테이블이 비어 있을 때만** 넣는다.
-- (따라서 퀴즈셋이 한 건이라도 있으면 아래 퀴즈셋/퀴즈 블록은 통째로 건너뛴다.)
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
-- 유저 3명
-- 비밀번호는 모두 `password123!` 의 BCrypt 해시다 (cost 10).
-- 인증 도입 전이라 당장 쓰이지 않지만, 나중에 그대로 로그인되도록 실제 해시를 넣어 둔다.
-- ──────────────────────────────────────────────────────────────────────────
INSERT INTO users (email, password, nickname, created_at, updated_at) VALUES
    ('blanken@blanken.io', '$2y$10$8wQ5/.EDz75v9fNDiPOux.awk0flP4dWpOeZ7.NLGYOBtFFaGHvnK', 'blanken',  now() - interval '90 days', now() - interval '90 days'),
    ('hyeon@blanken.io',   '$2y$10$8wQ5/.EDz75v9fNDiPOux.awk0flP4dWpOeZ7.NLGYOBtFFaGHvnK', '현이',     now() - interval '60 days', now() - interval '60 days'),
    ('mina@blanken.io',    '$2y$10$8wQ5/.EDz75v9fNDiPOux.awk0flP4dWpOeZ7.NLGYOBtFFaGHvnK', '미나',     now() - interval '30 days', now() - interval '30 days')
ON CONFLICT (email) DO NOTHING;


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
JOIN users u ON u.email = d.email
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
INSERT INTO quiz (quiz_set_id, sentence, answer_word, hint, created_at, updated_at)
SELECT s.id, p.sentence, p.answer_word, p.hint, now(), now()
FROM numbered s
CROSS JOIN LATERAL generate_series(1, s.quiz_count) AS g(n)
JOIN pool p ON p.n = ((s.idx + g.n - 1) % 16) + 1
WHERE NOT EXISTS (SELECT 1 FROM quiz);
