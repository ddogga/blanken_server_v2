package io.github.ddogga.blanken.config.auth.handler

import io.github.ddogga.blanken.config.auth.jwt.AuthTokenService
import io.github.ddogga.blanken.config.auth.jwt.RefreshTokenCookie
import io.github.ddogga.blanken.domain.user.UserRole
import io.github.ddogga.blanken.domain.user.UserStatus
import io.github.ddogga.blanken.dto.auth.CustomOauth2User
import io.github.ddogga.blanken.dto.auth.JwtProperties
import io.github.ddogga.blanken.dto.auth.Oauth2RedirectUrls
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component


/**
 *
 * OAuth2 로그인 성공 처리 (JWT 방식)
 * 1. Refresh Token 발급 + Redis 저장
 * 2. HttpOnly 쿠키로 설정
 * 3. 역할에 따라 프론트로 리다이렉트
 *
 * Access Token은 여기서 발급하지 않음. 리다이렉트 응답으로는 프론트에 안전하게 넘길 방법이 없음 -> 쿼리 파라미터로 보내면 히스토리와 로그에 남음.
 * 프론트가 도착 페이지에서 POST /auth/refresh를 호출해 쿠키로 Access Token을 받음.
 *
 */

@Component
class Oauth2LoginSuccessHandler(
    private val redirectUrls: Oauth2RedirectUrls,
    private val authTokenService: AuthTokenService,
    private val jwtProperties: JwtProperties,
) : SimpleUrlAuthenticationSuccessHandler(){


    private val log = LoggerFactory.getLogger(javaClass)
    private val redirectStrategy = DefaultRedirectStrategy()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication?.principal as? CustomOauth2User
            ?: run {
                log.error("OAuth2 로그인 성공 처리 불가 - 예상하지 못한 principal 타입: {}", authentication?.principal?.javaClass?.name)
                redirectStrategy.sendRedirect(request, response, "${redirectUrls.failure}?error=login_failed")
                return
            }

        // Refresh Token 발급(Redis 저장) -> 쿠키
        val refreshToken = authTokenService.issueRefreshToken(principal.userId)
        RefreshTokenCookie.add(response, refreshToken.value, jwtProperties.refreshTokenExpiry)

        // GUEST, PENDING 유저 확인 -> 가입 페이지로 이동

        // TODO: 새로운 유저 ROLE 추가시 분기 추가하기
        val targetUrl = when (principal.userRole to principal.userStatus) {
            UserRole.GUEST to UserStatus.PENDING -> redirectUrls.signup
            UserRole.USER to UserStatus.ACTIVE,
            UserRole.ADMIN to UserStatus.ACTIVE -> redirectUrls.success
            else -> redirectUrls.failure
        }

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}