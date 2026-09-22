package io.github.ddogga.blanken.controller

import com.ninjasquad.springmockk.MockkBean
import io.github.ddogga.blanken.domain.Quiz
import io.github.ddogga.blanken.dto.quiz.QuizResponse
import io.github.ddogga.blanken.exception.QuizNotFoundException
import io.github.ddogga.blanken.exception.QuizSetNotFoundExceptions
import io.github.ddogga.blanken.service.QuizService
import io.mockk.every
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put


@WebMvcTest(QuizController::class)
class QuizControllerTest(
    @Autowired private val mockMvc: MockMvc
) {

    @MockkBean
    private lateinit var quizService: QuizService

    @Test
    fun `201_퀴즈_생성_성공`() {
        // given
        every { quizService.create(QUIZ_SET_ID, any()) } returns quizResponse()

        // when & then
        mockMvc.post("/api/quiz-sets/$QUIZ_SET_ID/quizzes") {
            contentType = MediaType.APPLICATION_JSON
            content = REQUEST_BODY
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/api/quiz-sets/$QUIZ_SET_ID/quizzes/$QUIZ_ID") }
            jsonPath("$.id") { value(QUIZ_ID) }
            jsonPath("$.quizSetId") { value(QUIZ_SET_ID) }
            jsonPath("$.sentence") { value(QUIZ_SENTENCE) }
            jsonPath("$.answerWord") { value(QUIZ_ANSWER_WORD) }
            jsonPath("$.hint") { value(QUIZ_HINT) }
        }
    }

    @Test
    fun `404_존재하지_않는_퀴즈셋_퀴즈_생성_실패`() {
        // given
        every { quizService.create(QUIZ_SET_ID, any()) } throws QuizSetNotFoundExceptions(QUIZ_SET_ID)

        // when & then — 어떤 id 였는지는 로그로만 남고 응답에는 표준 메시지만 나간다.
        mockMvc.post("/api/quiz-sets/$QUIZ_SET_ID/quizzes") {
            contentType = MediaType.APPLICATION_JSON
            content = REQUEST_BODY
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("Q001") }
            jsonPath("$.message") { value("퀴즈 셋을 찾을 수 없습니다.") }
        }
    }


    @Test
    fun `400_퀴즈생성_빈칸_규칙_위반_실패`() {
        // when & then — 빈칸 {{}} 이 하나도 없는 문장
        mockMvc.post("/api/quiz-sets/$QUIZ_SET_ID/quizzes") {
            contentType = MediaType.APPLICATION_JSON
            content = BLANK_MISSING_REQUEST_BODY
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("C001") }
            jsonPath("$.fieldErrors[*].field") { value(hasItem("sentence")) }
            jsonPath("$.fieldErrors[*].message") { value(hasItem(Quiz.SENTENCE_RULE_MESSAGE)) }
        }
    }

    @Test
    fun `200_퀴즈_수정_성공`() {
        // given
        every { quizService.update(QUIZ_ID, QUIZ_SET_ID, any()) } returns quizResponse(
            sentence = UPDATED_SENTENCE,
            answerWord = UPDATED_ANSWER_WORD,
            hint = null,
        )

        // when & then — 생성과 달리 Location 이 없고 200 + 본문이다.
        mockMvc.put("/api/quiz-sets/$QUIZ_SET_ID/quizzes/$QUIZ_ID") {
            contentType = MediaType.APPLICATION_JSON
            content = UPDATE_REQUEST_BODY
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(QUIZ_ID) }
            jsonPath("$.quizSetId") { value(QUIZ_SET_ID) }
            jsonPath("$.sentence") { value(UPDATED_SENTENCE) }
            jsonPath("$.answerWord") { value(UPDATED_ANSWER_WORD) }
            // 힌트는 선택 항목이라 비우는 수정도 가능하다.
            jsonPath("$.hint") { value(null) }
        }
    }


    @Test
    fun `404_존재하지_않는_퀴즈_수정_실패`() {
        // given
        every { quizService.update(QUIZ_ID, QUIZ_SET_ID, any()) } throws QuizNotFoundException(QUIZ_ID)

        // when & then — 본문은 @Valid 를 통과하는 값이어야 서비스까지 도달해 404 가 나온다.
        mockMvc.put("/api/quiz-sets/$QUIZ_SET_ID/quizzes/$QUIZ_ID") {
            contentType = MediaType.APPLICATION_JSON
            content = UPDATE_REQUEST_BODY
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("Q002") }
            jsonPath("$.message") { value("퀴즈를 찾을 수 없습니다.") }
        }
    }


    @Test
    fun `200_소속_퀴즈셋_변경_성공`() {
        // given — 경로의 quizSetId 는 "옮겨 갈" 퀴즈셋이고, 응답의 quizSetId 도 그 값이어야 한다.
        every { quizService.changeQuizSet(QUIZ_ID, NEW_QUIZ_SET_ID) } returns quizResponse(quizSetId = NEW_QUIZ_SET_ID)

        // when & then — 본문 없는 PATCH.
        mockMvc.patch("/api/quiz-sets/$NEW_QUIZ_SET_ID/quizzes/$QUIZ_ID")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(QUIZ_ID) }
                jsonPath("$.quizSetId") { value(NEW_QUIZ_SET_ID) }
                jsonPath("$.sentence") { value(QUIZ_SENTENCE) }
            }
    }

    @Test
    fun `404_존재하지_않는_소속_퀴즈셋_변경_실패`() {
        // given — 옮겨 갈 대상 퀴즈셋이 없는 경우
        every { quizService.changeQuizSet(QUIZ_ID, NEW_QUIZ_SET_ID) } throws
                QuizSetNotFoundExceptions(NEW_QUIZ_SET_ID)

        // when & then
        mockMvc.patch("/api/quiz-sets/$NEW_QUIZ_SET_ID/quizzes/$QUIZ_ID")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("Q001") }
                jsonPath("$.message") { value("퀴즈 셋을 찾을 수 없습니다.") }
            }
    }

    private fun quizResponse(
        quizSetId: Long = QUIZ_SET_ID,
        sentence: String = QUIZ_SENTENCE,
        answerWord: String = QUIZ_ANSWER_WORD,
        hint: String? = QUIZ_HINT,
    ): QuizResponse = QuizResponse(
        id = QUIZ_ID,
        quizSetId = quizSetId,
        sentence = sentence,
        answerWord = answerWord,
        hint = hint,
    )

    companion object {
        private const val QUIZ_SET_ID = 1L
        private const val NEW_QUIZ_SET_ID = 2L
        private const val QUIZ_ID = 10L
        private const val QUIZ_SENTENCE = "She decided to {{}} the meeting until next week."
        private const val QUIZ_ANSWER_WORD = "postpone"
        private const val QUIZ_HINT = "미루다, 연기하다"
        private const val UPDATED_SENTENCE = "He tried to {{}} the deadline by two days."
        private const val UPDATED_ANSWER_WORD = "extend"

        private const val REQUEST_BODY =
            """{"sentence":"She decided to {{}} the meeting until next week.","answerWord":"postpone","hint":"미루다, 연기하다"}"""
        private const val BLANK_MISSING_REQUEST_BODY =
            """{"sentence":"She decided to postpone the meeting until next week.","answerWord":"postpone"}"""
        private const val UPDATE_REQUEST_BODY =
            """{"sentence":"He tried to {{}} the deadline by two days.","answerWord":"extend"}"""
    }
}
