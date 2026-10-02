package io.github.ddogga.blanken.config.auth.jwt

import io.github.ddogga.blanken.config.auth.AuthErrorCode
import io.github.ddogga.blanken.dto.auth.AccessTokenPayload
import io.github.ddogga.blanken.dto.auth.AuthUser
import io.github.ddogga.blanken.dto.auth.TokenResult
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter


/**
 * Authorization 헤더의 Access Token을 검증해 SecurityContext에 인증 정보를 설정한다.
 *
 * @Component로 빈등록은 하지 않음:
 *      Filter 타입 빈은 Spring Boot가 서블릿 필터로 자동 등록해 두 번 실행된다.
 *      따라서 SecurityConfig에서 직접 생성해 Security 필터 체인에만 넣는다.
 *
 * OncePerRequestFilter를 상속 : 토큰 검증을 중복으로 하지 않기 위해서 request attribute로 실행여부를 기록해 요청당 한 번만 실행되게 보장하는 OncePerRequestFilter를 상속
 */
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    private val securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        // 토큰 추출: 없으면 인증 정보 없이 통과하고 보호된 경로 접근시 인가 단계에서 401 반환
        val token = resolveToken(request)

        if (token != null) {
            when (val result = jwtTokenProvider.validateAccessToken(token)) {
                // 검증 성공: 인증 정보 설정
                is TokenResult.Valid -> setAuthentication(result.payload)

                // 실패: 예외를 던지지 않고 원인만 기록함. EntryPoint가 응답 코드로 사용
                TokenResult.Expired ->
                    request.setAttribute(AuthErrorCode.REQUEST_ATTRIBUTE, AuthErrorCode.TOKEN_EXPIRED)

                TokenResult.Invalid ->
                    request.setAttribute(AuthErrorCode.REQUEST_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN)
            }
        }

        // 어떤 경우든 다음 필터로 진행 (permitAll 경로는 토큰 상태와 무관하게 통과)
        filterChain.doFilter(request, response)
    }

    /** "Authorization: Bearer {token}" 형식일 때만 토큰 반환 */
    private fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }


    private fun setAuthentication(payload: AccessTokenPayload) {
        val principal = AuthUser(userId = payload.userId, userRole = payload.userRole)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${payload.userRole.name}"))

        // 이미 검증된 인증이므로 authenticated() 팩토리 사용 (credentials는 보관하지 않음)
        val authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)

        // 기존 context를 수정하지 않고 새 context를 만들어 교체함. (스레드 간 공유 문제 방지)
        val context = securityContextHolderStrategy.createEmptyContext()
        context.authentication = authentication
        securityContextHolderStrategy.context = context
    }


    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

}