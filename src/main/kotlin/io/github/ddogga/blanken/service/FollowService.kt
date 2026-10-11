package io.github.ddogga.blanken.service


import io.github.ddogga.blanken.domain.user.Follow
import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.dto.common.CursorResponse
import io.github.ddogga.blanken.dto.user.FollowerResponse
import io.github.ddogga.blanken.dto.user.FollowingResponse
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.FollowRepository
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class FollowService (
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun follow(followerId: Long, followeeId: Long) {

        val follow = Follow(
            follower = findUserById(followerId),
            followee = findUserById(followeeId),
        )
        followRepository.save(follow)
    }


    @Transactional
    fun unFollow(followerId: Long, followeeId: Long) {
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId)
    }


    /**
     * 해당 사용자가 팔로우한 유저 목록 조회
     */
    fun getFollows(userId: Long, cursor: Long?, size: Int): CursorResponse<FollowingResponse> {

        val follows = followRepository.getFollows(userId, cursor, size)

        val hasNext = follows.size > size
        val content = follows.take(size)

        return CursorResponse.from(
            content,
            if (hasNext) content.last().followId else null,
            hasNext
        )
    }

    fun getFollowers(userId: Long, cursor: Long?, size: Int): CursorResponse<FollowerResponse> {

        val followers = followRepository.getFollowers(userId, cursor, size)

        val hasNext = followers.size > size
        val content = followers.take(size)

        return CursorResponse.from(
            content,
            if (hasNext) content.last().followId else null,
            hasNext
        )
    }

    private fun findUserById(userId : Long): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)
    }


}