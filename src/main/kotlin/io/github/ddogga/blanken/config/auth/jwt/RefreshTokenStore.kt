package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.dto.auth.JwtProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component


/**
 * 발급한 Refresh Token을 Redis에 기록한다.
 *  key:    refresh_token: {tokenId(jti)}
 *  value: userId
 *  TTL:    Refresh Token 만료 시간 (만료되면 Redis에서도 자동 삭제)
 *
 * 토큰마다 키를 따로 두므로 한 회원이 여러 기기에 동시에 로그인 할 수 있음.
 */

@Component
class RefreshTokenStore(
    private val redisTemplate: StringRedisTemplate,
    private val jwtProperties: JwtProperties,
) {


    fun save(tokenId: String, userId: Long) {
        redisTemplate.opsForValue().set(key(tokenId), userId.toString(), jwtProperties.refreshTokenExpiry)
    }

    /**
     * Redis에서 꺼내면서 삭제. 원자적으로 동작하므로 같은 토큰을 동시에 재발급 요청해도 한 요청만 성공한다.
     *
     * @return 저장된 userId, 없으면(이미 사용했거나 로그아웃이거나 만료된 경우) null 처리
     */
    fun consume(tokenId: String): Long? =
        redisTemplate.opsForValue().getAndDelete(key(tokenId))?.toLongOrNull()


    fun delete(tokenId: String) {
        redisTemplate.delete(key(tokenId))
    }

    private fun key(tokenId: String) = "$KEY_PREFIX$tokenId"

    companion object {
        private const val KEY_PREFIX = "refresh_token:"
    }
}