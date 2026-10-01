package io.github.ddogga.blanken.controller

import com.ninjasquad.springmockk.MockkBean
import io.github.ddogga.blanken.config.TestSecurityConfig
import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.service.StudyHistoryService
import io.mockk.every
import org.springframework.http.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import kotlin.test.Test


@Import(TestSecurityConfig::class)
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
        }
    }

    @Test
    fun `400_문제별_결과_수와_푼_문제_수_불일치로_학습_히스토리_생성_실패`() {

        // when & then
        mockMvc.post("/api/study-histories") {
            contentType = MediaType.APPLICATION_JSON
            content = BAD_REQUEST_BODY_1
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("C001") }
            jsonPath("$.message") { value("입력값이 올바르지 않습니다.") }
            jsonPath("$.fieldErrors[0].field") { value("detailCountConsistent") }
            jsonPath("$.fieldErrors[0].message") { value("문제별 결과 수가 푼 문제 수와 일치해야 합니다.") }
        }
    }

    @Test
    fun `400_맞은_개수_풀이를_포기하지_않은_문제수_불일치로_학습_히스토리_생성_실패`() {

        // when & then
        mockMvc.post("/api/study-histories") {
            contentType = MediaType.APPLICATION_JSON
            content = BAD_REQUEST_BODY_2
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("C001") }
            jsonPath("$.message") { value("입력값이 올바르지 않습니다.") }
            jsonPath("$.fieldErrors[0].field") { value("correctCountConsistent") }
            jsonPath("$.fieldErrors[0].message") { value("맞은 개수와 풀이를 포기하지 않은 문제 수가 일치해야 합니다.") }
        }
    }



    private fun studyHistoryResponse(): StudyHistoryResponse = StudyHistoryResponse(
        id = STUDY_HISTORY_ID,
        userId = USER_ID,
        quizSetId = QUIZ_SET_ID,
        quizSetTitle = QUIZ_SET_TITLE,
        score = SCORE,
        totalCount = TOTAL_COUNT,
        correctCount = CORRECT_COUNT

    )

    companion object {
        private const val STUDY_HISTORY_ID = 1L
        private const val USER_ID = 2L
        private const val QUIZ_SET_ID = 3L
        private const val QUIZ_SET_TITLE = "토익 빈출 동사"
        private val SCORE = BigDecimal("80.00")
        private const val TOTAL_COUNT = 10
        private const val CORRECT_COUNT = 8
        private const val REQUEST_BODY = """                                                                                                                                                                 
              {                                                                                                                                                                                                
                "userId": $USER_ID,                                                                                                                                                                            
                "quizSetId": $QUIZ_SET_ID,                                                                                                                                                                     
                "totalCount": $TOTAL_COUNT,                                                                                                                                                                    
                "correctCount": $CORRECT_COUNT,                                                                                                                                                                
                "details": [                                                                                                                                                                                   
                  { "quizId": 1,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 2,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 3,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 4,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 5,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 6,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 7,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 8,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 9,  "gaveUp": true  },                                                                                                                                                           
                  { "quizId": 10, "gaveUp": true  }                                                                                                                                                            
                ]                                                                                                                                                                                              
              }                                                                                                                                                                                                
              """
        private const val BAD_REQUEST_BODY_1 = """                                                                                                                                                                 
              {                                                                                                                                                                                                
                "userId": $USER_ID,                                                                                                                                                                            
                "quizSetId": $QUIZ_SET_ID,                                                                                                                                                                     
                "totalCount": $TOTAL_COUNT,                                                                                                                                                                    
                "correctCount": $CORRECT_COUNT,                                                                                                                                                                
                "details": [                                                                                                                                                                                   
                  { "quizId": 1,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 2,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 3,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 4,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 5,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 6,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 7,  "gaveUp": false },
                  { "quizId": 8,  "gaveUp": false }
                ]                                                                                                                                                                                              
              }                                                                                                                                                                                                
              """
        private const val BAD_REQUEST_BODY_2 = """                                                                                                                                                                 
              {                                                                                                                                                                                                
                "userId": $USER_ID,                                                                                                                                                                            
                "quizSetId": $QUIZ_SET_ID,                                                                                                                                                                     
                "totalCount": $TOTAL_COUNT,                                                                                                                                                                    
                "correctCount": $CORRECT_COUNT,                                                                                                                                                                
                "details": [                                                                                                                                                                                   
                  { "quizId": 1,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 2,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 3,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 4,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 5,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 6,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 7,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 8,  "gaveUp": false },                                                                                                                                                           
                  { "quizId": 9,  "gaveUp": false  },                                                                                                                                                           
                  { "quizId": 10, "gaveUp": true  }                                                                                                                                                            
                ]                                                                                                                                                                                               
              }                                                                                                                                                                                                
              """

    }

}