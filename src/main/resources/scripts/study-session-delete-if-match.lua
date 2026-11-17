-- 세션 ID가 같을 때만 학습 세션을 삭제한다.
-- 완주 처리 중에 다른 기기에서 "처음부터"를 눌러 같은 키에 새 세션이 생겼다면, 그 새 세션은 지우지 않기 위함.
--
-- KEYS[1] : study:session:{userId}:{quizSetId}
-- ARGV[1] : 완주한 sessionId
--
-- 반환 : 1 = 삭제함, 0 = 세션이 없거나 다른 세션이라 삭제하지 않음
--
-- 키가 없으면 HGET은 nil(Lua에서 false)을 돌려주므로 비교가 실패해 0을 반환한다.

if redis.call('HGET', KEYS[1], 'sessionId') == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0