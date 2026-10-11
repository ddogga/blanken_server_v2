package io.github.ddogga.blanken.repository.querydsl

import io.github.ddogga.blanken.dto.user.FollowerResponse
import io.github.ddogga.blanken.dto.user.FollowingResponse

interface FollowCustomRepository {

    fun getFollows(followerId: Long, cursor: Long?, size: Int): List<FollowingResponse>

    fun getFollowers(followeeId: Long, cursor: Long?, size: Int): List<FollowerResponse>

}