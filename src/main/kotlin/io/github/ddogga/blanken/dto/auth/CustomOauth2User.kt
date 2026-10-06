package io.github.ddogga.blanken.dto.auth

import io.github.ddogga.blanken.domain.user.UserRole
import io.github.ddogga.blanken.domain.user.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOauth2User(
    val userId: Long,
    val userStatus: UserStatus,
    val userRole: UserRole,
    val oauth2Attributes: Map<String, Any>

    ) : OAuth2User {

    override fun getAttributes(): Map<String, Any> =
        oauth2Attributes.toMutableMap()

    // hasRole("USER")가 내부적으로 "ROLE_USER"의 형태로 권한을 검사하므로 접두사를 붙임
    override fun getAuthorities(): Collection<out GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority("ROLE_${userRole.name}"))

    // Principal 식별자. Provider ID가 아니라 우리 회원 ID를 사용
    override fun getName(): String = userId.toString()
}