package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.config.auth.AuthErrorCode
import io.github.ddogga.blanken.config.auth.jwt.AuthTokenService
import io.github.ddogga.blanken.config.auth.jwt.RefreshTokenCookie
import io.github.ddogga.blanken.config.auth.jwt.ReissueResult
import io.github.ddogga.blanken.dto.auth.JwtProperties
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/auth")
class AuthController (
    private val authTokenService: AuthTokenService,
    private val jwtProperties: JwtProperties,
) {

    /**
     * Refresh Token 쿠키로 Access Token 발급.
     * - 로그인 직후: 프론트가 리다이렉트된 페이지에서 바로 호출해 첫 Access Token을 받음.
     * - Access Token 만료(TOKEN_EXPIRED) 시: 재발급 후 원래 요청을 재시도함.
     */
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = RefreshTokenCookie.NAME, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Any> {
        val result = refreshToken?.let { authTokenService.reissue(it) } ?: ReissueResult.Failure

        return when (result) {
            is ReissueResult.Success -> {
                RefreshTokenCookie.add(response, result.refreshToken.value, jwtProperties.refreshTokenExpiry)

                ResponseEntity.ok(
                    AccessTokenResponse(
                        accessToken = result.accessToken,
                        expiresIn = jwtProperties.accessTokenExpiry.seconds
                    )
                )
            }

            ReissueResult.Failure -> {
                RefreshTokenCookie.delete(response)     // 쓸 수 없는 쿠키는 정리
                val code = AuthErrorCode.REFRESH_TOKEN_INVALID
                ResponseEntity.status(code.status).body(ErrorResponse(code.name, code.message))
            }
        }
    }

    /** Refresh Token 폐기 + 쿠키 삭제. Access Token은 만료까지 남으므로 프론트도 메모리에서 지워야 함.*/
    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = RefreshTokenCookie.NAME, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Unit> {
        refreshToken?.let { authTokenService.revoke(it) }
        RefreshTokenCookie.delete(response)
        return ResponseEntity.noContent().build()
    }

}

data class AccessTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,        // 초 단위. 프론트가 만료 전에 미리 재발급할 떄 참고
)

data class ErrorResponse(
    val code: String,
    val message: String,
)