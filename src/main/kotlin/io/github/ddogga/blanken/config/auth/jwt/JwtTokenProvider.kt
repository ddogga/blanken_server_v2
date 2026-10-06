package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.domain.user.UserRole
import io.github.ddogga.blanken.dto.auth.AccessTokenPayload
import io.github.ddogga.blanken.dto.auth.IssuedRefreshToken
import io.github.ddogga.blanken.dto.auth.JwtProperties
import io.github.ddogga.blanken.dto.auth.RefreshTokenPayload
import io.github.ddogga.blanken.dto.auth.TokenResult
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.crypto.SecretKey
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID


@Component
class JwtTokenProvider (
    private val properties: JwtProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 서버 기동 시점에 키 생성 -> 32바이트 미만이면 WeakKeyException으로 즉시 기동 실패
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret))

    private val parser: JwtParser = Jwts.parser().verifyWith(key).build()



    // ======== 토큰 발급 ==========

    fun createAccessToken(userId: Long, userRole: UserRole): String =
        buildToken(
            userId = userId,
            type = TokenType.ACCESS,
            expiry = properties.accessTokenExpiry,
            extraClaims = mapOf(ROLE_CLAIM to userRole.name)
        )


    /**
     * tokenId : UUID, Redis에 저장할 값
     */
    fun createRefreshToken(userId: Long): IssuedRefreshToken {
        val tokenId = UUID.randomUUID().toString()
        val value = buildToken(
            userId = userId,
            type = TokenType.REFRESH,
            expiry = properties.refreshTokenExpiry,
            tokenId = tokenId,
        )
        return IssuedRefreshToken(value = value, tokenId = tokenId)
    }

    private fun buildToken(
        userId: Long,
        type: TokenType,
        expiry: Duration,
        tokenId: String? = null,
        extraClaims: Map<String, Any> = emptyMap(),
    ): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim(TYPE_CLAIM, type.name)   // 용도 구분
            .claims(extraClaims)
            .apply { tokenId?.let { id(it) } }   // jti: Refresh Token 식별자
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiry)))
            .signWith(key)
            .compact()
    }


    // ========== 토큰 검증 ==========

    fun validateAccessToken(token: String): TokenResult<AccessTokenPayload> =
        parse(token, TokenType.ACCESS) {
            claims ->
            AccessTokenPayload(
                userId = requireNotNull(claims.subject).toLong(),
                userRole = UserRole.valueOf(requireNotNull(claims[ROLE_CLAIM, String::class.java])),
                )
        }

    fun validationRefreshToken(token: String): TokenResult<RefreshTokenPayload> =
        parse(token, TokenType.ACCESS) {
            claims ->
            RefreshTokenPayload(
                userId = requireNotNull(claims.subject).toLong(),
                tokenId = requireNotNull(claims.id),
            )
        }

    /**
     * 공통 파싱 흐름: 서명 검증 -> 만료 검사 -> 용도 확인 -> payload 변환.
     * jjwt는 서명을 먼저 검증한 뒤 만료를 검사. ExpiredJwtException은 서명은 정상이란 것을 의미
     */
    private fun <T> parse(
        token: String,
        expectedType: TokenType,
        toPayload: (Claims) -> T,
    ): TokenResult<T> =
        try {
            val claims = parser.parseSignedClaims(token).payload
            if (claims[TYPE_CLAIM] != expectedType.name) {
                TokenResult.Invalid
            } else {
                TokenResult.Valid(toPayload(claims))
            }
        } catch (e: ExpiredJwtException) {
            // 만료된 토큰도 용도 확인: 만료된 Refresh 토큰을 Access 자리에 넣어 Expired를 유도하는 것 방지
            if (e.claims[TYPE_CLAIM] == expectedType.name) {
                TokenResult.Expired
            } else {
                TokenResult.Invalid
            }
        } catch (e: JwtException) {
            // 서명 불일치, 형식 오류, 지원하지 않는 토큰 등
            log.debug("유효하지 않은 JWT: {}", e.message)
            TokenResult.Invalid
        } catch (e: IllegalArgumentException) {
            // 그외의 부적합한 요청 데이터 (빈문자열, sub가 숫자가 아니거나 알수 없는 role등)
            log.debug("JWT 처리 실패: {}", e.message)
            TokenResult.Invalid
        }


    companion object {
        private const val TYPE_CLAIM = "token_type"
        private const val ROLE_CLAIM = "role"
    }
}

enum class TokenType {
    ACCESS, REFRESH
}