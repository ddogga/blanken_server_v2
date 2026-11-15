-- 기존 학습 세션을 조회하면서 TTL을 연장한다 (이어풀기 진입 = 학습 활동으로 간주, 롤링 24h)
--
-- KEYS[1] : study:session:{userId}:{quizSetId}
-- ARGV[1] : TTL(초)
--
-- 반환 : 세션이 없으면 빈 배열
--        있으면 { '0', 남은TTL(ms), field1, value1, field2, value2, ... }
--        (첫 원소 '0' = 새로 생성하지 않았음. create 스크립트와 응답 형식을 맞추기 위함)

if redis.call('EXISTS', KEYS[1]) == 0 then
    return {}
end

redis.call('EXPIRE', KEYS[1], ARGV[1])

local reply = { '0', tostring(redis.call('PTTL', KEYS[1])) }
local hash = redis.call('HGETALL', KEYS[1])
for i = 1, #hash do
    reply[#reply + 1] = hash[i]
end
return reply

