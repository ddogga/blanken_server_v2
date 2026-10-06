package io.github.ddogga.blanken.dto.history

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.ddogga.blanken.domain.history.StudyHistory
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

    @field:Schema(description = "문제별 결과")
    @field:NotEmpty(message = "문제별 결과는 최소 1건이어야 합니다.")
    @field:Valid
    val details: List<StudyHistoryDetailRequest>,
) {
    @get:AssertTrue(message = "문제별 결과 수가 푼 문제 수와 일치해야 합니다.")
    @get:JsonIgnore // Swagger 및 JSON 직렬화 제외
    val isDetailCountConsistent: Boolean
        get() = details.size == totalCount

    @get:AssertTrue(message = "맞은 개수와 풀이를 포기하지 않은 문제 수가 일치해야 합니다.")
    @get:JsonIgnore // Swagger 및 JSON 직렬화 제외
    val isCorrectCountConsistent: Boolean
        get() = details.count { !it.gaveUp } == correctCount
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
) {
    companion object {
        fun from(studyHistory: StudyHistory): StudyHistoryResponse = StudyHistoryResponse(
            id = requireNotNull(studyHistory.id) {"저장되지 않은 StudyHistory는 응답으로 변환할 수 없습니다."},
            userId = requireNotNull(studyHistory.user.id) {"저장되지 않은 User는 응답으로 변환할 수 없습니다."},
            quizSetId = requireNotNull(studyHistory.quizSet.id) {"저장되지 않은 QuizSet은 응답으로 변환할 수 없습니다."},
            quizSetTitle = studyHistory.quizSet.title,
            score = studyHistory.score,
            totalCount = studyHistory.totalCount,
            correctCount = studyHistory.correctCount,
        )
    }
}


