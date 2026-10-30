-- 퀴즈셋 안에서의 퀴즈 순서 (0부터). Quiz.orderIndex 와 대응한다.
--
-- 기존 행은 퀴즈셋별로 id 순서(= 생성 순서)대로 0, 1, 2 ... 를 채운 뒤 NOT NULL 로 건다.
-- 엔티티가 Kotlin Int(논-널)라, NULL 이 남아 있으면 해당 행을 읽는 순간 매핑에 실패한다.
ALTER TABLE public.quiz ADD COLUMN order_index integer;

UPDATE public.quiz q
SET order_index = o.rn
FROM (
    SELECT id, row_number() OVER (PARTITION BY quiz_set_id ORDER BY id) - 1 AS rn
    FROM public.quiz
) o
WHERE q.id = o.id;

ALTER TABLE public.quiz ALTER COLUMN order_index SET NOT NULL;


-- 정답을 단어 하나(answer_word)에서 여러 단어 목록(answers)으로 바꾼다. Quiz.answers 와 대응한다.
--
-- 기존 행은 단일 정답을 원소 하나짜리 배열로 옮긴 뒤 NOT NULL 로 걸고, 옛 컬럼을 지운다.
-- 기존 퀴즈는 정답 1개 · 문장 단어 2개 이상이라 Quiz.validate 의 개수 규칙(정답 수 < 단어 수)을 그대로 만족한다.
ALTER TABLE public.quiz ADD COLUMN answers text[];

UPDATE public.quiz SET answers = ARRAY[answer_word];

ALTER TABLE public.quiz ALTER COLUMN answers SET NOT NULL;

ALTER TABLE public.quiz DROP COLUMN answer_word;


-- 퀴즈 문장의 번역. Quiz.translation 과 대응한다 (NOT NULL, 최대 500자).
--
-- 기존 행에는 번역이 없으므로 빈 문자열로 채운 뒤 NOT NULL 로 건다.
-- DEFAULT 는 기존 행을 채우는 용도로만 쓰고 바로 지운다 — 앞으로의 INSERT 는 애플리케이션이 값을 넣어야 한다.
ALTER TABLE public.quiz ADD COLUMN translation character varying(500) NOT NULL DEFAULT '';

ALTER TABLE public.quiz ALTER COLUMN translation DROP DEFAULT;
