package io.github.ddogga.blanken.config.auth.service

import io.github.ddogga.blanken.domain.UserStatus
import io.github.ddogga.blanken.dto.auth.CustomOauth2User
import io.github.ddogga.blanken.dto.auth.Oauth2UserInfo
import io.github.ddogga.blanken.dto.user.LoginUser
import io.github.ddogga.blanken.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service


/**
 *
 * oauth2Login()이 access token 교환을 마친 뒤 호출
 *
 * Provider(kakao,naver..) 별로 상이한 응답값을 공통 형태로 처리하기 위해 커스텀 서비스 로직 필요.
 * DefaultOAuth2UserService를 상속하여 구현
 *
 */

@Service
class CustomOAuth2UserService(
    private val userService: UserService,
) : DefaultOAuth2UserService() {


    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {

        // access token으로 user-info-url 호출 후 응답값 OAuth2 User 정보 가져오기
        val oauth2User = super.loadUser(userRequest)
        val attributes = oauth2User.attributes
        val providerName = userRequest.clientRegistration.registrationId

        // provider 별로 상이한 응답값 공통형태로 처리
        val oauth2UserInfo = Oauth2UserInfo.of(providerName, attributes)

        // (provider, providerId)로 회원 조회, 없으면 GUEST로 가입
        val loginUser = findOrRegister(oauth2UserInfo)
        if (loginUser.userStatus == UserStatus.WITHDRAWN) {
            throw OAuth2AuthenticationException(OAuth2Error("withdrawn_user"), "탈퇴한 회원입니다.")
        }

        // SuccessHandler에서 처리 분기를 위해 회원 ID와 역할을 담아 반환
        return CustomOauth2User(
            userId = loginUser.id,
            userStatus = loginUser.userStatus,
            userRole = loginUser.userRole,
            oauth2Attributes = oauth2User.attributes,
        )

    }


    private fun findOrRegister(userInfo: Oauth2UserInfo): LoginUser =
        userService.findLoginUser(userInfo.oauthProvider, userInfo.providerId)
            ?:userService.socialRegister(userInfo)


}