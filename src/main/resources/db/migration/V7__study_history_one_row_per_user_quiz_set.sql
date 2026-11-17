-- study_history 를 append-only(완주마다 1행)에서 (유저, 퀴즈셋)당 1행으로 바꾼다. StudyHistory 와 대응한다.
--
-- 완주할 때마다 행을 추가하지 않고 같은 행을 갱신한다:
--   score / total_count / correct_count / solved_at / 오답 목록 = 최근 완주 값으로 덮어쓰기
--   best_score / completion_count                             = 누적값
--   last_session_id                                           = 같은 세션의 완주가 두 번 반영되지 않게 하는 멱등 키
--   version                                                   = 낙관적 락 (@Version)
--
-- user_id / quiz_set_id 는 V5 그대로 NULL 허용 + ON DELETE SET NULL 이다 (유저·퀴즈셋이 지워져도 기록은 남긴다).


-- ──────────────────────────────────────────────────────────────────────────
-- 1. 새 컬럼 추가 (기존 행을 채운 뒤 NOT NULL 로 건다)
-- ──────────────────────────────────────────────────────────────────────────
ALTER TABLE public.study_history
    ADD COLUMN best_score       numeric(5,2),
    ADD COLUMN completion_count integer,
    ADD COLUMN last_session_id  character varying(255),
    ADD COLUMN version          bigint;

-- 점수는 0.00 ~ 100.00 이므로 엔티티(precision = 5, scale = 2)에 맞춰 좁힌다.
ALTER TABLE public.study_history ALTER COLUMN score TYPE numeric(5,2);


-- ──────────────────────────────────────────────────────────────────────────
-- 2. (유저, 퀴즈셋)별로 남길 행 고르기
--
-- 가장 최근에 완주한 행(solved_at, 같으면 id 가 큰 쪽)을 남기고, 그룹 전체에서 누적값을 계산한다.
-- user_id 나 quiz_set_id 가 NULL 인 행(삭제된 유저·퀴즈셋의 기록)은 UNIQUE 에 걸리지 않으므로
-- 병합 대상에서 빼고 각자 1회 완주로 둔다.
-- ──────────────────────────────────────────────────────────────────────────
CREATE TEMP TABLE study_history_dedup ON COMMIT DROP AS
SELECT id,
       row_number() OVER (PARTITION BY user_id, quiz_set_id ORDER BY solved_at DESC, id DESC) AS rn,
       max(score)   OVER (PARTITION BY user_id, quiz_set_id)                                  AS best,
       count(*)     OVER (PARTITION BY user_id, quiz_set_id)                                  AS cnt
FROM public.study_history
WHERE user_id IS NOT NULL
  AND quiz_set_id IS NOT NULL;

-- 남길 행에 누적값 반영
UPDATE public.study_history h
SET best_score       = d.best,
    completion_count = d.cnt
FROM study_history_dedup d
WHERE h.id = d.id
  AND d.rn = 1;

-- 버릴 행의 오답 목록 → 버릴 행 순서로 삭제 (오답 목록 FK 에 CASCADE 가 없다)
DELETE FROM public.study_history_wrong_revision w
USING study_history_dedup d
WHERE w.history_id = d.id
  AND d.rn > 1;

DELETE FROM public.study_history h
USING study_history_dedup d
WHERE h.id = d.id
  AND d.rn > 1;

-- 병합 대상이 아니었던 행(NULL 키)은 1회 완주로 채운다
UPDATE public.study_history
SET best_score       = score,
    completion_count = 1
WHERE best_score IS NULL;


-- ──────────────────────────────────────────────────────────────────────────
-- 3. 나머지 값 채우기
--
-- 기존 기록은 Redis 학습 세션 도입 전이라 세션 ID 가 없다.
-- 'legacy-{id}' 는 실제 세션 ID(UUID)와 겹칠 수 없어 isRecordedBy 판정에 영향을 주지 않는다.
-- ──────────────────────────────────────────────────────────────────────────
UPDATE public.study_history
SET last_session_id = 'legacy-' || id,
    version         = 0;


-- ──────────────────────────────────────────────────────────────────────────
-- 4. 제약 걸기
-- ──────────────────────────────────────────────────────────────────────────
ALTER TABLE public.study_history ALTER COLUMN best_score       SET NOT NULL;
ALTER TABLE public.study_history ALTER COLUMN completion_count SET NOT NULL;
ALTER TABLE public.study_history ALTER COLUMN last_session_id  SET NOT NULL;
ALTER TABLE public.study_history ALTER COLUMN version          SET NOT NULL;

ALTER TABLE public.study_history
    ADD CONSTRAINT uk_study_history_user_quiz_set UNIQUE (user_id, quiz_set_id);
