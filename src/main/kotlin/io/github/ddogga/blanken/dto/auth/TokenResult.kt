package io.github.ddogga.blanken.dto.auth

import io.github.ddogga.blanken.domain.UserRole


/**
 * 토큰 검증 결과를 담을 객체. 호출부는 when으로 세 가지 경우를 빠짐없이 처리함.
 * - Valid: 서명, 만료, 용도 모두 정상
 * - Expired: 서명은 정상이지만 만료됨 -> 프론트에서 재발급 시도
 * - Invalid: 위조, 형식 오류, 용도 불일치 -> 재발급 없이 재로그인
 */
sealed interface TokenResult<out T> {
    data class Valid<T>(val payload: T) : TokenResult<T>
    data object Expired : TokenResult<Nothing>
    data object Invalid : TokenResult<Nothing>
}

/** Access Token에서 꺼낸 인증 정보 */
data class AccessTokenPayload(
    val userId: Long,
    val userRole: UserRole,
)

/** Refresh Token에서 꺼낸 정보. tokenId(jti)는 Redis 저장값과 대조할 떄 사용 */
data class RefreshTokenPayload(
    val userId: Long,
    val tokenId: String
)

/** 발급한 Refresh Token을 담을 객체. 쿠키에 담을 값과 Redis에 저장할 식별자를 함께 반환 */
data class IssuedRefreshToken(
    val value: String,
    val tokenId: String,
)