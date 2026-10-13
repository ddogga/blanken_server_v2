package io.github.ddogga.blanken.controller

import com.ninjasquad.springmockk.MockkBean
import io.github.ddogga.blanken.config.TestSecurityConfig
import io.github.ddogga.blanken.dto.common.CursorResponse
import io.github.ddogga.blanken.dto.user.FollowerResponse
import io.github.ddogga.blanken.dto.user.FollowingResponse
import io.github.ddogga.blanken.service.FollowService
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import java.time.Instant


@Import(TestSecurityConfig::class)
@WebMvcTest(FollowController::class)
class FollowControllerTest(
    @Autowired private val mockMvc: MockMvc
) {

    @MockkBean
    private lateinit var followService: FollowService

    @Test
    fun `200_팔로잉_목록_조회_성공`() {

        // given
        every { followService.getFollows(USER_ID, null, CURSER_SIZE) } returns
                CursorResponse.from(listOf(followingResponse()), null, false)

        // when & then
        mockMvc.get("/api/users/$USER_ID/followings") {
            param("size", CURSER_SIZE.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.hasNext") { value(false) }
            jsonPath("$.content[0].followeeId") { value(FOLLOWEE_ID) }
            jsonPath("$.content[0].followId") { value(FOLLOW_ID) }
        }
    }

    @Test
    fun `200_팔로워_목록_조회_성공`() {
        // given
        every { followService.getFollowers(USER_ID, null, CURSER_SIZE) } returns
                CursorResponse.from(listOf(followerResponse()), null, false)

        // when & then
        mockMvc.get("/api/users/$USER_ID/followers") {
            param("size", CURSER_SIZE.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.hasNext") { value(false) }
            jsonPath("$.content[0].followerId") { value(FOLLOWER_ID) }
            jsonPath("$.content[0].followId") { value(FOLLOW_ID) }
        }
    }


    private fun followingResponse(): FollowingResponse = FollowingResponse(
        followeeId = FOLLOWEE_ID,
        nickname = FOLLOWEE_NICKNAME,
        followId = FOLLOW_ID,
        followedAt = Instant.now()
    )

    private fun followerResponse(): FollowerResponse = FollowerResponse(
        followerId = FOLLOWER_ID,
        nickname = FOLLOWER_NICKNAME,
        followId = FOLLOW_ID,
        followedAt = Instant.now()
    )


    companion object {
        private const val USER_ID = 1L
        private const val CURSER_SIZE = 10
        private const val FOLLOWEE_ID = 9L
        private const val FOLLOWEE_NICKNAME = "followee"
        private const val FOLLOW_ID = 2L
        private const val FOLLOWER_ID = 19L
        private const val FOLLOWER_NICKNAME = "follower"
    }

}