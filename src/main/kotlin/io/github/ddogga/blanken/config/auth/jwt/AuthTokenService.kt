package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.domain.UserStatus
import io.github.ddogga.blanken.dto.auth.IssuedRefreshToken
import io.github.ddogga.blanken.dto.auth.TokenResult
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 토큰 발급, 재발급, 페기
 * - 로그인 성공 시: Refresh Token만 발급 (리다이렉트로는 Access Token을 안전하게 전달할 수 없음)
 * - 재발급(/auth/refresh): Refresh Token 검증 -> DB에서 현재 역할, 상태 확인 -> Access + 새 Refresh 발급 (rotation)
 */
@Service
class AuthTokenService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val userRepository: UserRepository,
) {


    fun issueRefreshToken(userId: Long): IssuedRefreshToken {
        val issued = jwtTokenProvider.createRefreshToken(userId)
        refreshTokenStore.save(issued.tokenId, userId)
        return issued
    }


    @Transactional(readOnly = true)
    fun reissue(refreshToken: String): ReissueResult {

        // 서명, 만료, 용도 검증 (만료된 Refresh Token은 재로그인 대상이므로 Expired도 실패)
        val payload = when (val result = jwtTokenProvider
            .validationRefreshToken(refreshToken)) {
            is TokenResult.Valid -> result.payload
            TokenResult.Expired, TokenResult.Invalid -> return ReissueResult.Failure
        }

        // Redis에 살아 있는 토큰인지 확인하면서 삭제 (한 번 쓴 Refresh Token은 무효 -> rotation)
        val storedUserId = refreshTokenStore.consume(payload.tokenId)
        if (storedUserId != payload.userId) return ReissueResult.Failure

        // 토큰이 아니라 DB의 현재 역할, 상태로 발급 -> 가입 완료(GUEST -> USER), 탈퇴가 여기서 반영됨
        val user = userRepository.findByIdOrNull(payload.userId)
        if (user == null || user.userStatus == UserStatus.WITHDRAWN) return ReissueResult.Failure


        return ReissueResult.Success(
            accessToken = jwtTokenProvider.createAccessToken(payload.userId, user.userRole),
            refreshToken = issueRefreshToken(payload.userId)
        )
    }


    /** 로그아웃: 유효한 Refresh Token이면 Redis에서 삭제. 이미 무효한 토큰은 할 일이 없으므로 무시*/
    fun revoke(refreshToken: String) {
        when (val result = jwtTokenProvider.validationRefreshToken(refreshToken)) {
            is TokenResult.Valid -> refreshTokenStore.delete(result.payload.tokenId)
            TokenResult.Expired, TokenResult.Invalid -> Unit
        }
    }
}

sealed interface ReissueResult {
    data class Success(val accessToken: String, val refreshToken: IssuedRefreshToken) : ReissueResult
    data object Failure : ReissueResult
}