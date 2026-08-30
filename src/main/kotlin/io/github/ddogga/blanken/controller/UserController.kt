package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.user.PasswordChangeRequest
import io.github.ddogga.blanken.dto.user.UserCreateRequest
import io.github.ddogga.blanken.dto.user.UserResponse
import io.github.ddogga.blanken.dto.user.UserUpdateRequest
import io.github.ddogga.blanken.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI


@Tag(name = "User", description = "유저 CRUD API")
@RestController
@RequestMapping("/api/users")
class UserController(
	private val userService: UserService,
) {

	@Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임으로 유저를 생성합니다.")
	@PostMapping
	fun create(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
		val newUser = userService.create(request)
		return ResponseEntity.created(URI.create("/api/users/${newUser.id}")).body(newUser)
	}

	@Operation(summary = "유저 단건 조회")
	@GetMapping("/{id}")
	fun getById(
		@Parameter(description = "유저 ID", example = "1")
		@PathVariable id: Long,
	): UserResponse = userService.getById(id)

	@Operation(summary = "유저 목록 조회", description = "페이징하여 조회합니다.")
	@GetMapping
	fun getAll(
		@PageableDefault(size = 20) pageable: Pageable,
	): PageResponse<UserResponse> = userService.getAll(pageable)

	@Operation(summary = "닉네임 변경", description = "일부 필드만 바꾸므로 PATCH 입니다.")
	@PatchMapping("/{id}")
	fun updateNickname(
		@Parameter(description = "유저 ID", example = "1")
		@PathVariable id: Long,
		@Valid @RequestBody request: UserUpdateRequest,
	): UserResponse = userService.updateNickname(id, request)

	/**
	 * 현재 비밀번호 대조가 있어 같은 요청을 두 번 보내면 두 번째는 실패한다.
	 * 멱등하지 않으므로 PUT 이 아니라 POST 로 둔다.
	 */
	@Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 변경합니다.")
	@PostMapping("/{id}/password")
	fun changePassword(
		@Parameter(description = "유저 ID", example = "1")
		@PathVariable id: Long,
		@Valid @RequestBody request: PasswordChangeRequest,
	): ResponseEntity<Void> {
		userService.changePassword(id, request)
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "회원 탈퇴", description = "유저를 삭제합니다. 활동 이력이 있으면 삭제할 수 없습니다.")
	@DeleteMapping("/{id}")
	fun delete(
		@Parameter(description = "유저 ID", example = "1")
		@PathVariable id: Long,
	): ResponseEntity<Void> {
		userService.delete(id)
		return ResponseEntity.noContent().build()
	}
}
