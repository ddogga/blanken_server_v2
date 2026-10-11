package io.github.ddogga.blanken.repository.querydsl

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import io.github.ddogga.blanken.domain.user.QUser.user
import io.github.ddogga.blanken.domain.user.QFollow.follow
import io.github.ddogga.blanken.domain.user.UserStatus
import io.github.ddogga.blanken.dto.user.FollowerResponse
import io.github.ddogga.blanken.dto.user.FollowingResponse

@Repository
class FollowCustomRepositoryImpl (
    private val queryFactory: JPAQueryFactory
) : FollowCustomRepository{



    override fun getFollows(followerId: Long, cursor: Long?, size: Int): List<FollowingResponse> {

        return queryFactory
            .select(
                Projections.constructor(
                    FollowingResponse::class.java,
                    user.id,
                    user.nickname,
                    follow.id,
                    follow.createdAt
                )
            )
            .from(follow)
            .join(follow.followee, user)
            .where(
                follow.follower.id.eq(followerId),
                user.userStatus.eq(UserStatus.ACTIVE),
                cursor?.let { follow.id.lt(it) },
            )
            .orderBy(follow.id.desc())
            .limit(size.toLong() + 1)
            .fetch()
    }

    override fun getFollowers(followeeId: Long, cursor: Long?, size: Int): List<FollowerResponse> {

        return queryFactory
            .select(
                Projections.constructor(
                    FollowerResponse::class.java,
                    user.id,
                    user.nickname,
                    follow.id,
                    follow.createdAt
                )
            )
            .from(follow)
            .join(follow.follower, user)
            .where(
                follow.followee.id.eq(followeeId),
                user.userStatus.eq(UserStatus.ACTIVE),
                cursor?.let { follow.id.lt(it) },
            )
            .orderBy(follow.id.desc())
            .limit(size.toLong() + 1)
            .fetch()
    }
}