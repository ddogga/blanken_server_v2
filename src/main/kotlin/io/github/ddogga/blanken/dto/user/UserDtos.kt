package io.github.ddogga.blanken.dto.user

import io.github.ddogga.blanken.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(description = "회원가입 요청")
data class UserCreateRequest(

	@field:Schema(description = "이메일", example = "learner@blanken.io")
	@field:NotBlank(message = "이메일은 필수입니다.")
	@field:Email(message = "이메일 형식이 올바르지 않습니다.")
	@field:Size(max = 255, message = "이메일은 255자를 넘을 수 없습니다.")
	val email: String,

	@field:Schema(description = "비밀번호", example = "password1234")
	@field:NotBlank(message = "비밀번호는 필수입니다.")
	@field:Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
	val password: String,

	@field:Schema(description = "닉네임", example = "blanken")
	@field:NotBlank(message = "닉네임은 필수입니다.")
	@field:Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
	val nickname: String,
)

@Schema(description = "닉네임 변경 요청")
data class UserUpdateRequest(

	@field:Schema(description = "새 닉네임", example = "blanken2")
	@field:NotBlank(message = "닉네임은 필수입니다.")
	@field:Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
	val nickname: String,
)

@Schema(description = "비밀번호 변경 요청")
data class PasswordChangeRequest(

	@field:Schema(description = "현재 비밀번호")
	@field:NotBlank(message = "현재 비밀번호는 필수입니다.")
	val currentPassword: String,

	@field:Schema(description = "새 비밀번호")
	@field:NotBlank(message = "새 비밀번호는 필수입니다.")
	@field:Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
	val newPassword: String,
)

/**
 * 유저 응답. 비밀번호는 해시라 하더라도 절대 담지 않는다.
 */
@Schema(description = "유저 정보")
data class UserResponse(

	@field:Schema(description = "유저 ID", example = "1")
	val id: Long,

	@field:Schema(description = "이메일", example = "learner@blanken.io")
	val email: String,

	@field:Schema(description = "닉네임", example = "blanken")
	val nickname: String,

	@field:Schema(description = "가입 시각")
	val createdAt: Instant,
) {
	companion object {
		fun from(user: User): UserResponse = UserResponse(
			id = requireNotNull(user.id) { "저장되지 않은 User 는 응답으로 변환할 수 없습니다." },
			email = user.email,
			nickname = user.nickname,
			createdAt = user.createdAt,
		)
	}
}
