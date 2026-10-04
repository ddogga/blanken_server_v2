package io.github.ddogga.blanken.config.auth.repository

import io.github.ddogga.blanken.config.auth.jwt.Oauth2AuthorizationRequestStore
import io.github.ddogga.blanken.config.auth.util.CookieUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID


/**
 * OAuth2 인가 요청(state, redirect_uri 등)을 Redis에 저장하고 랜덤 키를 클라이언트에 반환한다.
 * 로그인 시작 요청(/oauth2/authorization/{id})과 콜백 요청(/login/oauth2/code/{id}) 사이를 이어주는 저장소
 */

@Component
class HttpCookieOAuth2AuthorizationRequestRepository(
    private val store: Oauth2AuthorizationRequestStore,
): AuthorizationRequestRepository<OAuth2AuthorizationRequest> {


    /** 쿠키의 랜덤 키로 Redis에서 조회 */
    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        resolveKey(request)?.let(store::find)

    /** 로그인 시 Redis에 요청 정보 저장. 쿠키에는 랜덤 키 발급해서 저장 */
    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        // 로그인 버튼을 여러 번 누른 경우 이전 시도의 Redis 값 정리
        resolveKey(request)?.let(store::delete)

        val key = UUID.randomUUID().toString()
        store.save(key, authorizationRequest, TTL)
        CookieUtils.addCookie(response, COOKIE_NAME, key, TTL)
    }

    /** 콜백 처리 시작 시 호출: Redis에서 꺼내면서 삭제(일회용) + 쿠키 삭제 */
    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? {
        val key = resolveKey(request) ?: return null
        CookieUtils.deleteCookie(response, COOKIE_NAME)
        return store.consume(key)
    }

    /** 로그인 실패 시 정리: 인가 요청을 꺼내기 전에 실패한 경우에도 쿠키와 Redis의 값을 함께 지워야 함.*/
    fun clear(request: HttpServletRequest, response: HttpServletResponse) {
        resolveKey(request)?.let(store::delete)
        CookieUtils.deleteCookie(response, COOKIE_NAME)
    }

    /**
     * 쿠키 값은 바뀔 수 있으므로 UUID 형식의 랜덤 키만 인정함.
     */
    private fun resolveKey(request: HttpServletRequest): String? =
        CookieUtils.getCookie(request, COOKIE_NAME)
            ?.value
            ?.takeIf { runCatching { UUID.fromString(it) }.isSuccess}

    companion object {
        const val COOKIE_NAME = "oauth2_auth_request"
        private val TTL: Duration = Duration.ofMinutes(3)   // 쿠키 만료 = Redis TTL
    }

}

