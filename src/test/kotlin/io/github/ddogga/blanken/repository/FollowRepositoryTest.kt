package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.config.QuerydslConfig
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@Import(PostgresTestContainerConfig::class, QuerydslConfig::class)
@Sql("/data.sql")
class FollowRepositoryTest (
    @Autowired private val followRepository: FollowRepository,
    @Autowired private val userRepository: UserRepository
) {

    @BeforeEach()
    fun init() {

        val follower = userRepository.findByEmail(FOLLOWER_EMAIL)
        val followee = userRepository.findByEmail(FOLLOWEE_EMAIL)

        FOLLOWER_ID = follower?.id!!
        FOLLOWEE_ID = followee?.id!!

    }


    @Test
    fun `해당_유저가_팔로우하고_있는 유저_목록을_반환한다`() {

        // when
        val follows = followRepository.getFollows(FOLLOWER_ID, null, CURSOR_SIZE)

        // then
        assertEquals(6, follows.size)
    }

    @Test
    fun `해당_유저를_팔로우하고_있는 유저_목록을_반환한다`() {

        // when
        val follows = followRepository.getFollowers(FOLLOWEE_ID, null, CURSOR_SIZE)

        // then
        assertEquals(6, follows.size)
    }


    companion object {
        var FOLLOWER_ID = 1L
        var FOLLOWEE_ID = 9L
        private const val FOLLOWER_EMAIL = "test@test.io"
        private const val FOLLOWEE_EMAIL = "blanken@blanken.io"
        private const val CURSOR_SIZE = 5
    }

}