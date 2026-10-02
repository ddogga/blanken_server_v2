package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.config.auth.AuthErrorCode
import io.github.ddogga.blanken.config.auth.writeAuthError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class JwtAccessDeniedHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.writeAuthError(AuthErrorCode.ACCESS_DENIED)
    }
}