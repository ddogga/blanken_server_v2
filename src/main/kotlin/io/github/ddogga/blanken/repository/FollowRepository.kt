package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.user.Follow
import io.github.ddogga.blanken.repository.querydsl.FollowCustomRepository
import org.springframework.data.jpa.repository.JpaRepository

interface FollowRepository : JpaRepository<Follow, Long>, FollowCustomRepository{


    fun deleteByFollowerIdAndFolloweeId(followerId: Long, followeeId: Long)

}