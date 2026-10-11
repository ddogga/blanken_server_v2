package io.github.ddogga.blanken.dto.user

import io.github.ddogga.blanken.domain.user.Follow
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant


/**
 * 해당 유저가 팔로우한 유저 정보
 */
@Schema(description = "팔로잉 정보")
data class FollowingResponse(

    @field:Schema(description = "해당 유저가 팔로잉한 유저 ID", example = "1")
    val followeeId: Long,

    @field:Schema(description = "해당 유저가 팔로잉한 유저 닉네임", example = "blanken")
    val nickname: String,

    @field:Schema(description = "팔로우 ID", example = "1")
    val followId: Long,

    @field:Schema(description = "팔로잉한 시간")
    val followedAt: Instant,
) {
    companion object {
        fun from(follow: Follow, nickname: String): FollowingResponse = FollowingResponse(
            followeeId = requireNotNull(follow.followee.id) { "저장되지 않은 User는 응답으로 변환할 수 없습니다." },
            nickname = nickname,
            followId = requireNotNull(follow.id) { "저장되지 않은 Follow 정보는 응답으로 변환할 수 없습니다. "},
            followedAt = follow.createdAt,
        )
    }
}

/**
 * 해당 유저를 팔로우한 유저 정보
 */
@Schema(description = "팔로워 정보")
data class FollowerResponse(

    @field:Schema(description = "해당 유저를 팔로우한 유저 ID", example = "1")
    val followerId: Long,

    @field:Schema(description = "해당 유저를 팔로우한 유저 닉네임", example = "blanken")
    val nickname: String,

    @field:Schema(description = "팔로우 ID", example = "1")
    val followId: Long,

    @field:Schema(description = "팔로우한 시간")
    val followedAt: Instant,
) {
    companion object {
        fun from(follow: Follow, nickname: String): FollowerResponse = FollowerResponse(
            followerId = requireNotNull(follow.follower.id) { "저장되지 않은 User는 응답으로 변환할 수 없습니다." },
            nickname = nickname,
            followId = requireNotNull(follow.id) { "저장되지 않은 Follow 정보는 응답으로 변환할 수 없습니다. "},
            followedAt = follow.createdAt,
        )
    }
}