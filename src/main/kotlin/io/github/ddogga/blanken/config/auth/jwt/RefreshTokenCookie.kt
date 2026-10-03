package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.config.auth.util.CookieUtils
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration

/**
 * Refresh Token 쿠키 규칙 :
 * 발급(SuccessHandler와 재발급)과 삭제(로그아웃, 재발급 실패)가 같은 이름,경로를 써야 삭제가 동작함.
 */
object RefreshTokenCookie {
    const val NAME = "refresh_token"

    // /auth/refresh, /auth/logout 요청에만 전송 -> 일반 API 요청에는 Refresh Token이 실리지 않음.
    const val PATH = "/auth"

    fun add(response: HttpServletResponse, token: String, maxAge: Duration) {
        CookieUtils.addCookie(response, NAME, token, maxAge, PATH)
    }

    fun delete(response: HttpServletResponse) {
        CookieUtils.deleteCookie(response, NAME, PATH)
    }
}