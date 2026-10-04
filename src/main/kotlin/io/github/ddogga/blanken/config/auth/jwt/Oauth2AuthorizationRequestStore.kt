package io.github.ddogga.blanken.config.auth.jwt

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64
import java.time.Duration


/**
 * OAuth2 인가 요청을 Redis에 보관함.
 *  key: oauth2_auth_request: {랜덤 키}
 *  value: Java 직렬화 -> Base64 문자열
 *  TTL: 로그인 시작 - 콜백까지 허용 시간
 *
 */

@Component
class Oauth2AuthorizationRequestStore(
    private val redisTemplate: StringRedisTemplate,
) {

    fun save(key: String, authorizationRequest: OAuth2AuthorizationRequest, ttl: Duration) {
        redisTemplate.opsForValue().set(redisKey(key), serialize(authorizationRequest), ttl)
    }

    fun find(key: String): OAuth2AuthorizationRequest? =
        redisTemplate.opsForValue().get(redisKey(key))?.let(::deserialize)


    /** 꺼내면서 삭제(GETDEL). 같은 콜백이 두 번 와도 한 번만 성공 */
    fun consume(key: String): OAuth2AuthorizationRequest? =
        redisTemplate.opsForValue().getAndDelete(redisKey(key))?.let(::deserialize)

    fun delete(key: String) {
        redisTemplate.delete(redisKey(key))
    }

    private fun redisKey(key: String) = "$KEY_PREFIX$key"

    private fun serialize(request: OAuth2AuthorizationRequest): String {
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(request) }
            bos.toByteArray()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun deserialize(value: String): OAuth2AuthorizationRequest? =
        runCatching {
            val bytes = Base64.getDecoder().decode(value)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
                    as? OAuth2AuthorizationRequest
        }.getOrNull()

    companion object {
        private const val KEY_PREFIX = "oauth2_auth_request:"
    }

}