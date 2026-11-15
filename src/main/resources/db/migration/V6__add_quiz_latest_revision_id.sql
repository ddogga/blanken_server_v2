-- 퀴즈의 최신 리비전(스냅샷) id. Quiz.latestRevisionId 와 대응한다.
--
-- 엔티티가 Long? 이라 NULL 을 허용한다 — 퀴즈를 먼저 INSERT 해 id 를 얻은 뒤에야 리비전을 만들 수 있어서,
-- 생성 직후 잠깐은 값이 비어 있다.
ALTER TABLE public.quiz ADD COLUMN latest_revision_id bigint;

-- 기존 퀴즈는 가장 최근 리비전으로 채운다.
-- V5 에서 모든 기존 퀴즈에 첫 스냅샷을 만들었으므로 빈 행은 남지 않는다.
UPDATE public.quiz q
SET latest_revision_id = r.max_id
FROM (
    SELECT quiz_id, max(id) AS max_id
    FROM public.quiz_revision
    WHERE quiz_id IS NOT NULL
    GROUP BY quiz_id
) r
WHERE q.id = r.quiz_id;
