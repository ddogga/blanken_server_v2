package io.github.ddogga.blanken.dto.quiz

import io.github.ddogga.blanken.domain.User
import io.swagger.v3.oas.annotations.media.Schema


@Schema(description = "유저 정보")
data class UserResponse(

    @field:Schema(description = "유저 ID", example = "1")
    val id: Long,

    @field:Schema(description = "유저 이메일", example = "user@mail.com")
    val email: String,

    @field:Schema(description = "유저 닉네임", example = "user1")
    val nickname: String,

) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = requireNotNull(user.id) { "저장되지 않은 User는 응답으로 변환할 수 없습니다."},
            email = user.email,
            nickname = user.nickname
        )
    }
}