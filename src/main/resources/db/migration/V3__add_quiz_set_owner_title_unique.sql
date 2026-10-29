-- 같은 소유자 안에서 퀴즈셋 제목 중복을 DB가 원자적으로 거부한다.
-- QuizSetService.checkTitleDuplication 선검사는 메시지 품질용일 뿐, 동시 요청은 막지 못한다 (check-then-act).
--
-- 컬럼 순서는 (owner_id, title) — 제약이 만드는 인덱스를 owner_id 단독 조회(내 퀴즈셋 목록)도 함께 쓰게 하기 위해서다.
-- 이름은 QuizSet 엔티티의 @UniqueConstraint 와 맞춘다.
ALTER TABLE public.quiz_set
    ADD CONSTRAINT quiz_set_owner_id_title UNIQUE (owner_id, title);
