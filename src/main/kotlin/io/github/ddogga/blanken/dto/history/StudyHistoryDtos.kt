package io.github.ddogga.blanken.dto.history

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizRevisionRequest
import io.github.ddogga.blanken.dto.quiz.QuizRevisionResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal


@Schema(description = "학습 히스토리 생성 요청")
data class StudyHistoryRequest(

    @field:Schema(description = "학습한 유저 ID", example = "1")
    @field:Positive(message = "유저 ID는 양수여야 합니다.")
    val userId: Long,

    @field:Schema(description = "학습한 퀴즈셋 ID", example = "1")
    @field:Positive(message = "퀴즈셋 ID는 양수여야 합니다.")
    val quizSetId: Long,

    @field:Schema(description = "푼 문제 수", example = "10")
    @field:Positive(message = "푼 문제 수는 양수여야 합니다.")
    val totalCount: Int,

    @field:Schema(description = "맞힌 문제 수", example = "8")
    @field:PositiveOrZero(message = "맞힌 문제 수는 0 이상이어야 합니다.")
    val correctCount: Int,

    @field:Schema(description = "틀린 퀴즈의 리비전 ID 목록 — 학습 세션에 담긴 스냅샷 id", example = "[12, 57]")
    @field:Valid
    val wrongRevisionIds: List<Long>,
) {
    @get:AssertTrue(message = "틀린 퀴즈 수와 맞힌 문제수의 합이 푼 문제수와 일치해야 합니다.")
    @get:JsonIgnore // Swagger 및 JSON 직렬화 제외
    val isTotalCountConsistent: Boolean
        get() = wrongRevisionIds.size + correctCount == totalCount
}

@Schema(description = "학습 히스토리 응답")
data class StudyHistoryResponse(

    @field:Schema(description = "학습 히스토리 ID", example = "8")
    val id: Long,

    @field:Schema(description = "학습한 유저 ID", example = "1")
    val userId: Long,

    @field:Schema(description = "학습한 퀴즈셋 ID", example = "1")
    val quizSetId: Long,

    @field:Schema(description = "학습한 퀴즈셋 제목", example = "토익 기출 단어")
    val quizSetTitle: String,

    @field:Schema(description = "점수", example = "80")
    val score: BigDecimal,

    @field:Schema(description = "푼 문제 수", example = "10")
    val totalCount: Int,

    @field:Schema(description = "맞힌 문제 수", example = "8")
    val correctCount: Int,

    @field:Schema(description = "틀린 퀴즈 목록")
    val wrongQuizzes: List<QuizRevisionResponse>,

    )


