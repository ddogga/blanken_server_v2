package io.github.ddogga.blanken.dto.auth

import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * Oauth2 로그인 처리 후 브라우저를 돌려보낼 프론트엔트 주소.
 */

@ConfigurationProperties(prefix = "app.oauth2.redirect")
data class Oauth2RedirectUrls(
    val signup: String,     // 신규 회원(GUEST): 추가 정보 입력 페이지
    val success: String,    // 가입 완료 회원(USER/ADMIN): 로그인 후 첫 화면
    val failure: String,    // 로그인 실패: 로그인 페이지
)