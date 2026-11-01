package io.github.ddogga.blanken.dto.quiz

import io.github.ddogga.blanken.domain.quiz.Quiz
import io.github.ddogga.blanken.domain.quiz.QuizRevision
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "퀴즈 스냅샷 (revision) 요청")
data class QuizRevisionRequest(

    @field:Schema(description = "퀴즈 리비전 ID", example = "1")
    @field:Positive(message = "퀴즈 리비전ID는 양수여야 합니다.")
    val id: Long,

    @field:Schema(description = "퀴즈 ID", example = "1")
    @field:Positive(message = "퀴즈 ID는 양수여야 합니다.")
    val quizId: Long,

    @field:Schema(
        description = "빈칸 {{}} 이 하나 이상 포함된 영어 문장 (중괄호는 빈칸 표시 전용)",
        example = "She decided to {{}} the meeting until next week.",
    )
    @field:NotBlank(message = "문장은 필수 입니다.")
    @field:Size(max = 500, message = "문장은 500자를 넘을 수 없습니다.")
    @field:Pattern(regexp = Quiz.SENTENCE_PATTERN, message = Quiz.SENTENCE_RULE_MESSAGE)
    val sentence: String,

    @field:Schema(description = "정답 단어 목록", example = "[\"postpone\"]")
    val answers: List<String>,

    @field:Schema(description = "힌트 (선택)", example = "미루다, 연기하다", nullable = true)
    @field:Size(max = 200, message = "힌트는 200자를 넘을 수 없습니다.")
    val hint: String? = null,

    @field:Schema(description = "문장 번역", example = "그녀는 회의를 다음 주로 미루기로 했다.")
    @field:Size(max = 500, message = "번역 문장은 500자를 넘을 수 없습니다.")
    val translation: String,
)


@Schema(description = "퀴즈 스냅샷 (revision) 정보")
data class QuizRevisionResponse(

    @field:Schema(description = "퀴즈 리비젼 ID", example = "1")
    val id: Long,

    @field:Schema(description = "퀴즈 ID", example = "1")
    val quizId: Long?,

    @field:Schema(
        description = "빈칸 {{}} 이 포함된 영어 문장",
        example = "She decided to {{}} the meeting until next week.",
    )
    val sentence: String,

    @field:Schema(description = "정답 단어 목록", example = "[postpone]")
    val answers: List<String>,

    @field:Schema(description = "힌트 (선택)", example = "미루다, 연기하다", nullable = true)
    val hint: String?,

    @field:Schema(description = "문장 번역", example = "그녀는 회의를 다음 주로 미루기로 했다.")
    val translation: String,

    ) {
    companion object {
        fun from(quizRevision: QuizRevision): QuizRevisionResponse = QuizRevisionResponse(
            id = requireNotNull(quizRevision.id) {"저장되지 않은 Quiz Revision은 응답으로 변환할 수 없습니다."},
            quizId = quizRevision.quizId,
            sentence = quizRevision.sentence,
            answers = quizRevision.answers,
            hint = quizRevision.hint,
            translation = quizRevision.translation,
        )
    }
}