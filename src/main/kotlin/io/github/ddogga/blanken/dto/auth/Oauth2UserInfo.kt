package io.github.ddogga.blanken.dto.auth

import io.github.ddogga.blanken.domain.OauthProvider


data class Oauth2UserInfo (

    val oauthProvider: OauthProvider,
    val providerId:String,
    val email: String?,
    val nickName: String?,
    val picture: String?,

    ) {

    companion object {
        fun of(providerName: String, attributes: Map<String, Any>): Oauth2UserInfo {

            val provider = OauthProvider.from(providerName)
            return provider.extract(attributes)
        }
    }

}