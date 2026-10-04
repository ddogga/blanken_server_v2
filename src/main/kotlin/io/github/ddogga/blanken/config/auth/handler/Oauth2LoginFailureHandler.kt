package io.github.ddogga.blanken.config.auth.handler


import io.github.ddogga.blanken.config.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository
import io.github.ddogga.blanken.dto.auth.Oauth2RedirectUrls
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder


/**
 *
 * OAuth2 로그인 실패 처리
 *
 * 프론트 로그인 페이지로 리다이렉트하면서 에러 코드만 쿼리 파라미터로 전달.
 *
 * SimpleUrlAuthenticationFailureHandler 사용하지 않음 :
 * 예외를 세션에 저장하고 고정된 실패 URL을 보내야 함. 프론트가 따로 있으므로
 * 에러 코드를 퀴리 파라미터로 직접 전달해야 하므로 인터페이스를 직접 구현
 *
 */


@Component
class Oauth2LoginFailureHandler(
    private val redirectUrls: Oauth2RedirectUrls,
    private val authorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository,
) : AuthenticationFailureHandler{

    private val log = LoggerFactory.getLogger(javaClass)
    private val redirectStrategy = DefaultRedirectStrategy()


    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorCode = resolveErrorCode(exception)
        log.warn(
            "OAuth2 로그인 실패 code={} uri={} query={}",
            errorCode, request.requestURI, request.queryString, exception
        )

        authorizationRequestRepository.clear(request, response)

        val targetUrl = UriComponentsBuilder
            .fromUriString(redirectUrls.failure)
            .queryParam("error", errorCode)
            .build()
            .encode()
            .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    /**
     * 프론트에는 내부 메세지 대신 정해진 에러 코드만 노출.
     * access_denied: 사용자가 제공자 동의 회면에서 취소 했을 떄
     * withdrawn_user / unsupported_provider / invalid_user_info: CustomOauth2UserService에서 던진 코드
     * 그 외 OAuth2 오류(invalid_client, authorization_request_not_found 등)는 원래 코드 그대로
     */

    private fun resolveErrorCode(exception: AuthenticationException): String =
        (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: DEFAULT_ERROR_CODE


    companion object {
        private const val DEFAULT_ERROR_CODE = "login_failed"
    }

}

