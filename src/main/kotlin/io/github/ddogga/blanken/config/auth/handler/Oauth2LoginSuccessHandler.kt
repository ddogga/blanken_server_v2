package io.github.ddogga.blanken.config.auth.handler

import io.github.ddogga.blanken.domain.UserRole
import io.github.ddogga.blanken.domain.UserStatus
import io.github.ddogga.blanken.dto.auth.CustomOauth2User
import io.github.ddogga.blanken.dto.auth.Oauth2RedirectUrls
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component


/**
 *
 * OAuth2 로그인 성공 처리 (세션 방식)
 * 인증 정보는 Security가 이미 세션에 저장했으므로, 여기서는 역할에 따라 리다이렉트할 곳만 정한다.
 *
 */

@Component
class Oauth2LoginSuccessHandler(
    private val redirectUrls: Oauth2RedirectUrls,
) : SimpleUrlAuthenticationSuccessHandler(){


    private val log = LoggerFactory.getLogger(javaClass)

    override fun determineTargetUrl(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ): String {
        val principal = authentication?.principal as? CustomOauth2User
            ?: run {
                log.error("예상하지 못한 principal 타입: {}", authentication?.principal?.javaClass?.name)
                return "${redirectUrls.failure}?error=login_failed"
            }

        // GUEST, PENDING 유저 확인 -> 가입 페이지로 이동

        // TODO: 새로운 유저 ROLE 추가시 분기 추가하기
        return when (principal.userRole to principal.userStatus) {
            UserRole.GUEST to UserStatus.PENDING -> redirectUrls.signup
            UserRole.USER to UserStatus.ACTIVE,
            UserRole.ADMIN to UserStatus.ACTIVE -> redirectUrls.success
            else -> redirectUrls.failure
        }

    }
}