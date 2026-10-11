package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.dto.auth.AuthUser
import io.github.ddogga.blanken.dto.common.CursorResponse
import io.github.ddogga.blanken.dto.user.FollowingResponse
import io.github.ddogga.blanken.service.FollowService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Follow", description = "팔로우 관리 API")
@RestController
@RequestMapping("/api/users/{userId}")
class FollowController(
    private val followService: FollowService,
) {

    @Operation(summary = "팔로우 요청", description = "팔로우 정보를 생성합니다.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/follow")
    fun follow(
        @Parameter(description = "팔로우할 대상 유저 ID", example = "2")
        @PathVariable userId: Long,
        @AuthenticationPrincipal authUser: AuthUser,
    ) = followService.follow(authUser.userId, userId)


    @Operation(summary = "팔로우 해제", description = "팔로우 정보를 삭제 합니다.")
    @DeleteMapping("/follow")
    fun unFollow(
        @PathVariable userId: Long,
        @AuthenticationPrincipal principal: UserPrincipal,
    ) {}

    @Operation(summary = "팔로잉 목록 조회", description = "해당 유저가 팔로우한 유저 목록을 조회합니다.")
    @GetMapping("/followings")
    fun getFollows(
        @PathVariable userId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ) {}

    @Operation(summary = "팔로워 목록 조회", description = "해당 유저를 팔로우한 유저 목록을 조회힙니다.")
    @GetMapping("/followers")
    fun getFollowers(
        @PathVariable userId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
    ) {}

}