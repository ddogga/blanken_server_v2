package io.github.ddogga.blanken.controller

import com.ninjasquad.springmockk.MockkBean
import io.github.ddogga.blanken.config.TestSecurityConfig
import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.dto.quiz.QuizRevisionResponse
import io.github.ddogga.blanken.exception.QuizRevisionNotFoundException
import io.github.ddogga.blanken.service.StudyHistoryService
import io.mockk.every
import org.springframework.http.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import kotlin.test.Test


@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
@WebMvcTest(StudyHistoryController::class)
class StudyHistoryControllerTest (
  @Autowired private val mockMvc: MockMvc
) {

    @MockkBean
    private lateinit var studyHistoryService: StudyHistoryService


    @Test
    fun `201_학습_히스토리_생성_성공`() {

        // given
        every { studyHistoryService.create(any()) } returns studyHistoryResponse()

        // when & then
        mockMvc.post("/api/study-histories") {
            contentType = MediaType.APPLICATION_JSON
            content = REQUEST_BODY
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/api/study-histories/$STUDY_HISTORY_ID") }
            jsonPath("$.id") { value(STUDY_HISTORY_ID)}
            jsonPath("$.userId") { value(USER_ID) }
            jsonPath("$.quizSetId") { value(QUIZ_SET_ID) }
            jsonPath("$.quizSetTitle") { value(QUIZ_SET_TITLE) }
            // BigDecimal 을 그대로 넘기면 scale 때문에 어긋나므로 Double 로 비교한다.
            jsonPath("$.score") { value(SCORE.toDouble()) }
            jsonPath("$.totalCount") { value(TOTAL_COUNT) }
            jsonPath("$.correctCount") { value(CORRECT_COUNT) }
            jsonPath("$.wrongQuizzes.length()") { value(2) }
            jsonPath("$.wrongQuizzes[0].id") { value(REVISION_ID_1) }
            jsonPath("$.wrongQuizzes[0].answers[0]") { value(ANSWER) }
            jsonPath("$.wrongQuizzes[1].id") { value(REVISION_ID_2) }
        }
    }

    @Test
    fun `400_오답_수와_맞힌_수의_합이_푼_문제_수와_불일치로_학습_히스토리_생성_실패`() {

        // when & then — 오답 1 + 맞힘 8 = 9 ≠ 푼 문제 10
        mockMvc.post("/api/study-histories") {
            contentType = MediaType.APPLICATION_JSON
            content = COUNT_MISMATCH_REQUEST_BODY
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("C001") }
            jsonPath("$.message") { value("입력값이 올바르지 않습니다.") }
            jsonPath("$.fieldErrors[0].field") { value("totalCountConsistent") }
            jsonPath("$.fieldErrors[0].message") { value("틀린 퀴즈 수와 맞힌 문제수의 합이 푼 문제수와 일치해야 합니다.") }
        }
    }

    @Test
    fun `404_존재하지_않는_오답_리비전으로_학습_히스토리_생성_실패`() {

        // given
        every { studyHistoryService.create(any()) } throws QuizRevisionNotFoundException(setOf(REVISION_ID_2))

        // when & then — 없는 id 는 응답에 담지 않는다 (진단용으로 로그에만 남는다)
        mockMvc.post("/api/study-histories") {
            contentType = MediaType.APPLICATION_JSON
            content = REQUEST_BODY
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("Q006") }
            jsonPath("$.message") { value("퀴즈 스냅샷을 찾을 수 없습니다.") }
        }
    }


    private fun studyHistoryResponse(): StudyHistoryResponse = StudyHistoryResponse(
        id = STUDY_HISTORY_ID,
        userId = USER_ID,
        quizSetId = QUIZ_SET_ID,
        quizSetTitle = QUIZ_SET_TITLE,
        score = SCORE,
        totalCount = TOTAL_COUNT,
        correctCount = CORRECT_COUNT,
        wrongQuizzes = listOf(revisionResponse(REVISION_ID_1), revisionResponse(REVISION_ID_2)),
    )

    private fun revisionResponse(id: Long): QuizRevisionResponse = QuizRevisionResponse(
        id = id,
        quizId = id + 100,
        sentence = SENTENCE,
        answers = listOf(ANSWER),
        hint = null,
        translation = TRANSLATION,
    )

    companion object {
        private const val STUDY_HISTORY_ID = 1L
        private const val USER_ID = 2L
        private const val QUIZ_SET_ID = 3L
        private const val QUIZ_SET_TITLE = "토익 빈출 동사"
        private val SCORE = BigDecimal("80.00")
        private const val TOTAL_COUNT = 10
        private const val CORRECT_COUNT = 8
        private const val REVISION_ID_1 = 11L
        private const val REVISION_ID_2 = 12L
        private const val SENTENCE = "She decided to {{}} the meeting until next week."
        private const val ANSWER = "postpone"
        private const val TRANSLATION = "그녀는 회의를 다음 주로 미루기로 했다."

        /** 오답 2 + 맞힘 8 = 푼 문제 10. 오답은 학습 세션이 들고 있던 리비전 id 다. */
        private const val REQUEST_BODY = """
              {
                "userId": $USER_ID,
                "quizSetId": $QUIZ_SET_ID,
                "totalCount": $TOTAL_COUNT,
                "correctCount": $CORRECT_COUNT,
                "wrongRevisionIds": [$REVISION_ID_1, $REVISION_ID_2]
              }
              """

        /** 오답 1 + 맞힘 8 = 9 ≠ 푼 문제 10 */
        private const val COUNT_MISMATCH_REQUEST_BODY = """
              {
                "userId": $USER_ID,
                "quizSetId": $QUIZ_SET_ID,
                "totalCount": $TOTAL_COUNT,
                "correctCount": $CORRECT_COUNT,
                "wrongRevisionIds": [$REVISION_ID_1]
              }
              """
    }

}