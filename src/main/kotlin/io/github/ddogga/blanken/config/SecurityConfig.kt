package io.github.ddogga.blanken.config

import io.github.ddogga.blanken.config.auth.handler.Oauth2LoginFailureHandler
import io.github.ddogga.blanken.config.auth.handler.Oauth2LoginSuccessHandler
import io.github.ddogga.blanken.config.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository
import io.github.ddogga.blanken.config.auth.service.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole
import org.springframework.security.authorization.SingleResultAuthorizationManager.permitAll
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oauth2LoginSuccessHandler: Oauth2LoginSuccessHandler,
    private val oauth2LoginFailureHandler: Oauth2LoginFailureHandler,
    private val cookieOAuth2AuthorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository
) {


    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {

        http {

            formLogin { disable() }
            httpBasic { disable() }

            csrf { disable() }

            // 세션 생성 - 로그인 성공시 authentication을 세션에 저장
            // 이후 요청은 JSESSIONID 쿠키로 세션을 찾아 인증 정보를 복원.
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED
                sessionFixation { changeSessionId() } // 세션 고정 공격을 막기위해 인증 성공시 세션 ID 새로 발급 - 기본 설정
            }

            // 인가 규칙
            authorizeHttpRequests {

                // OAuth2 로그인 시작, 콜백 (oauth2Login 필터가 처리)
                authorize("/oauth2/**", permitAll)
                authorize("/login/oauth2/**", permitAll)

                // 컨트롤러 예외 포워딩 경로 (막으면 원래 에러가 401로 가려짐)
                authorize("/error", permitAll)


                // Swagger UI + OpenAPI 문서
                authorize("/swagger-ui.html", permitAll)
                authorize("/swagger-ui/**", permitAll)     // index.html, js/css 등 정적 리소스
                authorize("/v3/api-docs", permitAll)
                authorize("/v3/api-docs/**", permitAll)    // swagger-config 포함

                authorize("/admin/**", hasRole("ADMIN"))
                authorize(anyRequest, permitAll )
//                authorize(anyRequest, hasRole("USER")) // GUEST 차단 - 운영 환경에서 주석 해제 TODO: 추후 프로파일로 개발, 운영 환경 분리
            }

            // OAuth2 로그인 (인가 요청은 세션 대신 쿠키에 저장 - JWT 전환을 위해)
            oauth2Login {
                authorizationEndpoint {
                    authorizationRequestRepository = cookieOAuth2AuthorizationRequestRepository
                }
                userInfoEndpoint {
                    userService = customOAuth2UserService
                }
                authenticationSuccessHandler = oauth2LoginSuccessHandler
                authenticationFailureHandler = oauth2LoginFailureHandler
            }

            // 로그아웃


            // 미인증 요청

        }
        return http.build()
    }



}