package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.config.auth.AuthErrorCode
import io.github.ddogga.blanken.config.auth.writeAuthError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * 인증되지 않은 요청이 보호된 경로에 접근할 때 호출됨 - 401 에러
 * 필터가 남긴 실패 원인이 있으면 그 코드로, 없으면(토큰 없음) UNAUTHORIZED로 응답한다.
 */
@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val code = request.getAttribute(AuthErrorCode.REQUEST_ATTRIBUTE) as? AuthErrorCode
            ?: AuthErrorCode.UNAUTHORIZED
        response.writeAuthError(code)
    }
}