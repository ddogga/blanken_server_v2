package io.github.ddogga.blanken.dto.history

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.ddogga.blanken.domain.history.StudyHistory
import io.github.ddogga.blanken.domain.quiz.QuizRevision
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
import java.time.Instant


@Schema(description = "학습 세션 완주 결과 (서버 내부 모델). Redis에서 조회")
data class StudyCompletion(
    @field:Schema(description = "완주한 학습 세션 ID (UUID). StudyHistory.lastSessionId로 복사되어 멱등 키로 쓰임", example = "3f2b8c1e-7d4a-4e9b-a1c2-5d6e7f8a9b0c")
    val sessionId: String,

    @field:Schema(description = "완주 시점의 퀴즈셋 제목 스냅샷", example = "토익 필수 구동사")
    val quizSetTitle: String,

    @field:Schema(description = "세션의 전체 문제 수 (1 이상)", example = "20")
    val totalCount: Int,

    @field:Schema(description = "맞힌 문제 수 (재입력 끝에 맞힌 문제 포함). correctCount + 오답 수 = totalCount", example = "17")
    val correctCount: Int,

    @field:Schema(description = "포기한 문제의 리비전 목록. 세션 스냅샷의 revisionId 기준 (학습 당시 버전)")
    val wrongRevisions: List<QuizRevision>,

    @field:Schema(description = "완주 처리 시각", example = "2026-11-17T05:16:00Z")
    val completedAt: Instant,
) {
    init {
        require(totalCount > 0) { "totalCount must be positive" }
        require(correctCount + wrongRevisions.size == totalCount) {
            "correctCount + wrong count must equal totalCount"
        }
    }
}



@Schema(description = "학습 히스토리 (완주 응답)")
data class StudyHistoryResponse(
    @field:Schema(description = "히스토리 ID", example = "3")
    val historyId: Long,

    @field:Schema(description = "퀴즈셋 ID", example = "1")
    val quizSetId: Long?,

    @field:Schema(description = "완주 시점의 퀴즈셋 제목", example = "토익 필수 구동사")
    val quizSetTitle: String,

    @field:Schema(description = "이번(최근) 점수, 0.00~100.00", example = "85.00")
    val score: BigDecimal,

    @field:Schema(description = "최고 점수", example = "90.00")
    val bestScore: BigDecimal,

    @field:Schema(description = "전체 문제 수", example = "20")
    val totalCount: Int,

    @field:Schema(description = "맞힌 문제 수 (재입력 끝에 맞힌 문제 포함)", example = "17")
    val correctCount: Int,

    @field:Schema(description = "이 퀴즈셋을 완주한 횟수", example = "3")
    val completionCount: Int,

    @field:Schema(description = "최근 완주 시각", example = "2026-11-17T04:50:00Z")
    val solvedAt: Instant,

    @field:Schema(description = "최근 완주에서 포기한 퀴즈의 리비전 ID 목록 (오답노트 대상)", example = "[340, 355]")
    val wrongRevisionIds: List<Long>,
) {
    companion object {
        /** wrongQuizzes가 LAZY 로딩이므로 트랜잭션 안에서 호출해야 한다 */
        fun from(history: StudyHistory) = StudyHistoryResponse(
            historyId = checkNotNull(history.id),
            quizSetId = history.quizSetId,
            quizSetTitle = history.quizSetTitle,
            score = history.score,
            bestScore = history.bestScore,
            totalCount = history.totalCount,
            correctCount = history.correctCount,
            completionCount = history.completionCount,
            solvedAt = history.solvedAt,
            wrongRevisionIds = history.wrongQuizzes.map { checkNotNull(it.id) },
        )
    }
}

