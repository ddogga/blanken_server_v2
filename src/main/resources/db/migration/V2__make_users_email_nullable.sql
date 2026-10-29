-- 소셜 제공자가 이메일을 주지 않을 수 있다 (카카오 account_email 은 비즈 앱 전용 동의 항목, 사용자 제공 거부 등).
-- 이메일을 모르는 회원에게 더미 값을 넣지 않고 NULL 로 둔다 — 값이 있는 것처럼 보이는 가짜 이메일을 남기지 않기 위해서다.
ALTER TABLE public.users ALTER COLUMN email DROP NOT NULL;
