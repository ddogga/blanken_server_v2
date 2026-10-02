package io.github.ddogga.blanken.config.auth.repository

import io.github.ddogga.blanken.config.auth.util.CookieUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.stereotype.Component
import java.time.Duration


/**
 * OAuth2 인가 요청(state, redirect_uri 등)을 세션 대신 쿠키에 보관한다.
 * 로그인 시작 요청(/oauth2/authorization/{id})과 콜백 요청(/login/oauth2/code/{id}) 사이를 이어주는 저장소
 */

@Component
class HttpCookieOAuth2AuthorizationRequestRepository: AuthorizationRequestRepository<OAuth2AuthorizationRequest> {


    /** 콜백 요청에서 쿠키를 역질렬화해서 객체로 인가 요청 복원 */
    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        CookieUtils.getCookie(request, COOKIE_NAME)
            ?.let { CookieUtils.deserialize<OAuth2AuthorizationRequest>(it.value)}

    /** 로그인 시작 요청에서 인가 요청을 쿠키로 저장. null이면 삭제를 의미 */
    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {

        CookieUtils.addCookie(
            response = response,
            name = COOKIE_NAME,
            value = CookieUtils.serialize(authorizationRequest),
            maxAge = COOKIE_MAX_AGE
        )
    }

    /** 콜백 처리 시작 시 호출: 꺼내 쓰고 바로 삭제 - 일회용 */
    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? =
        loadAuthorizationRequest(request).also {
            CookieUtils.deleteCookie(response, COOKIE_NAME)
        }


    companion object {
        const val COOKIE_NAME = "oauth2_auth_request"
        private val COOKIE_MAX_AGE: Duration = Duration.ofMinutes(3) // Provider 로그인, 동의에 걸리는 시간
    }

}

