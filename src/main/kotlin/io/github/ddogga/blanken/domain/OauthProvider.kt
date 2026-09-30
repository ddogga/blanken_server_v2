package io.github.ddogga.blanken.domain

import io.github.ddogga.blanken.dto.auth.Oauth2UserInfo
import io.github.ddogga.blanken.exception.ErrorCode
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error


/**
 * Provider(kakao, naver등)별로 응답값이 상이 하므로
 * 공통형태로 변환하는 로직 추가
 */

enum class OauthProvider(val providerName: String) {


    /**
     * { "id": 123456789,
     *   "kakao_account":
     *          { "email": "...", "profile":
     *              { "nickname": "...", "profile_image_url": "https://..." } } }
     */

    KAKAO("kakao") {
        override fun extract(attributes: Map<String, Any>): Oauth2UserInfo {

            val account = attributes["kakao_account"] as? Map<*, *>
                ?: throw invalidOauth2UserInfo("kakao_account 데이터가 존재하지 않습니다.")
            val profile = account["profile"] as? Map<*, *>
                ?: throw invalidOauth2UserInfo("kakao_account.profile 데이터가 존재하지 않습니다.")

            return Oauth2UserInfo(
                oauthProvider = this,
                providerId = attributes["id"]?.toString()   // Long으로 오지만 String으로 통일
                    ?: throw invalidOauth2UserInfo("kakao provider id가 존재하지 않습니다."),
                email = account["email"]?.toString(),
                nickName = profile["nickname"]?.toString(),
                picture = profile["profile_image_url"]?.toString()
            )
        }
    },


    /**
     * { "resultcode": "00", "message": "success",
     *      "response": { "id": "abc...", "nickname": "...", "email": "...", "profile_image": "https://..." } }
     */
    NAVER("naver") {
        override fun extract(attributes: Map<String, Any>): Oauth2UserInfo {
            val response = attributes["response"] as? Map<*, *>
                ?: throw invalidOauth2UserInfo("naver provider의 response 데이터가 존재하지 않습니다.")

            return Oauth2UserInfo(
                oauthProvider = this,
                providerId = response["id"]?.toString()
                    ?: throw invalidOauth2UserInfo("naver provider의 id가 존재하지 않습니다."),
                email = response["email"]?.toString(),
                nickName = response["nickname"]?.toString(),
                picture = response["profile_image"]?.toString()
            )
        }
    };


    abstract fun extract(attributes: Map<String, Any>): Oauth2UserInfo


    companion object {
        fun from(providerName: String): OauthProvider =
            entries.find { it.providerName == providerName}
                ?: throw invalidOauth2UserInfo("지원하지 않는 제공자입니다: $providerName")
    }

}


private fun invalidOauth2UserInfo(message: String) =
    OAuth2AuthenticationException(OAuth2Error
        (ErrorCode.INVALID_OAUTH2_USER_INFO.message), message)