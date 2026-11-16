-- 학습 세션에 문제별 결과를 저장하고 TTL을 연장한다 (24h).
--
-- KEYS[1] : study:session:{userId}:{quizSetId}
-- ARGV[1] : TTL(초)
-- ARGV[2] : sessionId (클라이언트가 들고 있는 세션 ID)
-- ARGV[3..] : quizId, result, quizId, result, ... (쌍으로 반복)
--
-- 반환
--   { 'OK', 남은TTL(ms), 푼 문제 수, 전체 문제 수 }
--   { 'NOT_FOUND' }            : 세션이 없음 (만료 또는 존재하지 않음)
--   { 'SESSION_MISMATCH' }     : 같은 키에 다른 세션이 있음 (다른 기기에서 처음부터 다시 시작함)
--   { 'INVALID_QUIZ', quizId } : 이 세션에 없는 문제
--
-- 검증을 모두 통과해야만 저장한다. 하나라도 실패하면 아무것도 저장하지 않는다.

if redis.call('EXISTS', KEYS[1]) == 0 then
    return { 'NOT_FOUND' }
end

if redis.call('HGET', KEYS[1], 'sessionId') ~= ARGV[2] then
    return { 'SESSION_MISMATCH' }
end

-- 세션 스냅샷에 들어 있는 문제 ID 집합 (cjson: Redis에 내장된 JSON 라이브러리)
local quizzes = cjson.decode(redis.call('HGET', KEYS[1], 'quizzes'))
local valid = {}
for _, quiz in ipairs(quizzes) do
    valid[quiz.quizId] = true
end

-- 1) 검증만 먼저
for i = 3, #ARGV, 2 do
    if not valid[tonumber(ARGV[i])] then
        return { 'INVALID_QUIZ', ARGV[i] }
    end
end

-- 2) 통과하면 한 번에 저장
local fields = {}
for i = 3, #ARGV, 2 do
    fields[#fields + 1] = 'r:' .. ARGV[i]
    fields[#fields + 1] = ARGV[i + 1]
end
redis.call('HSET', KEYS[1], unpack(fields))
redis.call('EXPIRE', KEYS[1], ARGV[1])

-- 3) 진행률 계산
local solved = 0
for _, quiz in ipairs(quizzes) do
    if redis.call('HEXISTS', KEYS[1], 'r:' .. quiz.quizId) == 1 then
        solved = solved + 1
    end
end

return { 'OK', tostring(redis.call('PTTL', KEYS[1])), tostring(solved), tostring(#quizzes) }
