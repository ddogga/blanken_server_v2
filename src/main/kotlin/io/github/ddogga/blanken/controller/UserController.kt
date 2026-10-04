package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.config.auth.jwt.JwtTokenProvider
import io.github.ddogga.blanken.dto.auth.AuthUser
import io.github.ddogga.blanken.dto.auth.JwtProperties
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.user.PasswordChangeRequest
import io.github.ddogga.blanken.dto.user.SignupRequest
import io.github.ddogga.blanken.dto.user.SignupResult
import io.github.ddogga.blanken.dto.user.UserResponse
import io.github.ddogga.blanken.dto.user.UserUpdateRequest
import io.github.ddogga.blanken.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Tag(name = "User", description = "유저 CRUD API")
@RestController
@RequestMapping("/api/users")
class UserController(
	private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties,
) {

//	@Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임으로 유저를 생성합니다.")
//	@PostMapping
//	fun create(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
//		val newUser = userService.create(request)
//		return ResponseEntity.created(URI.create("/api/users/${newUser.id}")).body(newUser)
//	}

    @Operation(summary = "회원가입", description = "")
    @PostMapping("/signup")
    fun signup(
        @AuthenticationPrincipal authUser: AuthUser,
        @Valid @RequestBody request: SignupRequest,
    ): ResponseEntity<Any> {
        val response = userService.signup(authUser.userId, request)

        if (response.signupResult == SignupResult.COMPLETED) {
            return ResponseEntity.ok(AccessTokenResponse(
                accessToken = jwtTokenProvider.createAccessToken(response.userId!!, response.userRole!!),
                expiresIn = jwtProperties.accessTokenExpiry.seconds
            ))
        } else if (response.signupResult == SignupResult.ALREADY_COMPLETED) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse("SIGNUP_ALREADY_COMPLETED", "이미 가입이 완료된 회원입니다."))
        } else if (response.signupResult == SignupResult.USER_NOT_FOUND) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("USER_NOT_FOUND", "회원 정보를 찾을 수 없습니다."))
        } else throw RuntimeException("잘못된 회원가입 응답 정보")
    }



	@Operation(summary = "유저 단건 조회")
	@GetMapping
	fun getById(
        @AuthenticationPrincipal authUser: AuthUser
	): UserResponse = userService.getById(authUser.userId)


	@Operation(summary = "유저 목록 조회", description = "페이징하여 조회합니다.")
	@GetMapping
	fun getAll(
		@PageableDefault(size = 20) pageable: Pageable,
	): PageResponse<UserResponse> = userService.getAll(pageable)


	@Operation(summary = "닉네임 변경", description = "닉네임을 변경합니다.")
	@PatchMapping("/nickname")
	fun updateNickname(
		@AuthenticationPrincipal authUser: AuthUser,
		@Valid @RequestBody request: UserUpdateRequest,
	): UserResponse = userService.updateNickname(authUser.userId, request)

	/**
	 * 현재 비밀번호 대조가 있어 같은 요청을 두 번 보내면 두 번째는 실패한다.
	 * 멱등하지 않으므로 PUT 이 아니라 POST 로 둔다.
	 */
//	@Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 변경합니다.")
//	@PostMapping("/password")
//	fun changePassword(
//		@AuthenticationPrincipal authUser: AuthUser,
//		@Valid @RequestBody request: PasswordChangeRequest,
//	): ResponseEntity<Void> {
//		userService.changePassword(authUser.userId, request)
//		return ResponseEntity.noContent().build()
//	}

	@Operation(summary = "회원 탈퇴", description = "유저를 삭제합니다. 활동 이력이 있으면 삭제할 수 없습니다.")
	@DeleteMapping
	fun delete(
		@AuthenticationPrincipal authUser: AuthUser
	): ResponseEntity<Void> {
		userService.delete(authUser.userId)
		return ResponseEntity.noContent().build()
	}
}
