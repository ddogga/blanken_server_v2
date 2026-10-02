package io.github.ddogga.blanken.dto.auth

import io.github.ddogga.blanken.domain.UserRole

/**
 * JWT 인증 후 SecurityContext에 담김
 * 컨트롤러에서 @AuthenticationPrincipal authUser: AuthUser로 꺼내 쓸 수 있음.
 */
data class AuthUser(
    val userId: Long,
    val userRole: UserRole,
)