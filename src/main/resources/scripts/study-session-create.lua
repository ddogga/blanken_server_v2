-- 학습 세션을 "없을 때만" 생성한다. 조회와 생성 사이에 다른 요청이 끼어들 수 없도록 Lua로 묶음.
--
-- KEYS[1] : study:session:{userId}:{quizSetId}
-- ARGV[1] : TTL(초)
-- ARGV[2] : sessionId
-- ARGV[3] : quizzes (JSON 문자열)
-- ARGV[4] : startedAt (epoch seconds)
--
-- 반환 : { created('1' 또는 '0'), 남은TTL(ms), field1, value1, ... }
--   '1' = 이번 요청이 새로 만듦
--   '0' = 그 사이 다른 기기가 먼저 만들어 둠 → 기존 세션을 그대로 반환 (덮어쓰지 않음)

local created = '0'

if redis.call('EXISTS', KEYS[1]) == 0 then
    redis.call('HSET', KEYS[1],
        'sessionId', ARGV[2],
        'quizzes',   ARGV[3],
        'startedAt', ARGV[4])
    created = '1'
end

-- 새로 만들었든 기존 것이든 학습 활동이 있었으므로 TTL을 갱신 (롤링 24h)
redis.call('EXPIRE', KEYS[1], ARGV[1])

local reply = { created, tostring(redis.call('PTTL', KEYS[1])) }
local hash = redis.call('HGETALL', KEYS[1])
for i = 1, #hash do
    reply[#reply + 1] = hash[i]
end
return reply

