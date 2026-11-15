package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.dto.study.QuizSnapshot
import tools.jackson.module.kotlin.readValue
import tools.jackson.databind.json.JsonMapper
import io.github.ddogga.blanken.dto.study.StudyResult
import io.github.ddogga.blanken.dto.study.StudySession
import io.github.ddogga.blanken.dto.study.StudySessionStartResult
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant

@Repository
class StudySessionRedisRepository(
    private val redisTemplate: StringRedisTemplate, // 키, 값, 스크립트, 인자/결과를 모두 문자열로 직렬화 함

    private val jsonMapper: JsonMapper,
) {


    private val getAndExtendScript: RedisScript<List<*>> = loadScript("scripts/study-session-touch-and-get.lua")
    private val createScript: RedisScript<List<*>> = loadScript("scripts/study-session-create.lua")


    /** 세션이 있으면 TTL을 연장하고 반환, 없으면 null */
    fun getAndExtendTtl(userId: Long, quizSetId: Long): StudySession? {
        val reply = redisTemplate.execute(
            getAndExtendScript,
            listOf(key(userId, quizSetId)),
            TTL.seconds.toString(),
        )
        if (reply.isNullOrEmpty()) return null
        return parse(reply).session
    }


    /**
     * 세션이 없을 때만 생성한다.
     * 동시에 다른 요청이 먼저 생성헀다면 덮어쓰지 않고 그 세션을 반환한다(create=false)
     */
    fun createIfAbsent(
        userId: Long,
        quizSetId: Long,
        sessionId: String,
        quizzes: List<QuizSnapshot>,
        startedAt: Instant
    ): StudySessionStartResult {
        val reply = redisTemplate.execute(
            createScript,
            listOf(key(userId, quizSetId)),
            TTL.seconds.toString(),
            sessionId,
            jsonMapper.writeValueAsString(quizzes),
            startedAt.epochSecond.toString(),
        ) ?: throw IllegalStateException("Redis script returned null")
        return parse(reply)
    }

    /**
     * 처음 부터 다시 - 기존 세션 폐기
     */
    fun delete(userId: Long, quizSetId: Long) {
        redisTemplate.delete(key(userId, quizSetId))
    }


    /**
     * 스크립트 응답을 도메인 객체로 변환
     *
     * 스크립트 응답 구조:
     * [세션 생성 여부] [ttl] [session result hash] -> 앞에 2개는 메타 데이터
     */
    private fun parse(reply: List<*>): StudySessionStartResult {
        val values = reply.map { it.toString() }
        val created = values[0] == "1"
        val pttlMillis = values[1].toLong()

        val fields: Map<String, String> = values.drop(2) // 앞에 두 메타데이터는 버림.
            .chunked(2) // key, value씩 짝지서 2중 리스트 만듬.
            .associate { (field, value) -> field to value} // 각 원소로 map 만듬.

        val results = fields
            .filterKeys { it.startsWith(RESULT_PREFIX) }
            .map { (field, value) -> field.removePrefix(RESULT_PREFIX).toLong() to StudyResult.valueOf(value) }
            .toMap()

        val session = StudySession(
            sessionId = fields.getValue("sessionId"),
            quizzes = jsonMapper.readValue(fields.getValue("quizzes")),
            startedAt = Instant.ofEpochSecond(fields.getValue("startedAt").toLong()),
            results = results,
            expiresAt = Instant.now().plusMillis(pttlMillis),
        )
        return StudySessionStartResult(session, created)
    }


    private fun loadScript(path: String): RedisScript<List<*>> =
        DefaultRedisScript<List<*>>().apply {
            setLocation(ClassPathResource(path))
            resultType = List::class.java
        }


    companion object {
        val TTL: Duration = Duration.ofHours(24)
        const val RESULT_PREFIX = "r:"

        fun key(userId: Long, quizSetId: Long) = "study:session:$userId:$quizSetId"
    }
}