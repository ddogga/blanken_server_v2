package io.github.ddogga.blanken.dto.quiz

import io.github.ddogga.blanken.domain.Quiz
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "퀴즈 요청")
data class QuizRequest(

    /**
     * 빈칸 자리를 [Quiz.BLANK] 로 표시한 문장. 클라이언트가 빈칸 위치를 정해 보내므로 서버는 단어를 찾지 않는다.
     * 플레이스홀더가 정확히 하나여야 하며, 중괄호는 플레이스홀더 전용이라 다른 위치에는 올 수 없다.
     */
    @field:Schema(
        description = "빈칸 {{}} 이 정확히 하나 포함된 영어 문장 (중괄호는 빈칸 표시 전용)",
        example = "She decided to {{}} the meeting until next week.",
    )
    @field:NotBlank(message = "문장은 필수 입니다.")
    @field:Size(max = 500, message = "문장은 500자를 넘을 수 없습니다.")
    @field:Pattern(regexp = Quiz.SENTENCE_PATTERN, message = Quiz.SENTENCE_RULE_MESSAGE)
    val sentence: String,

    @field:Schema(description = "정답 단어", example = "postpone")
    @field:NotBlank(message = "정답 단어는 필수 입니다.")
    @field:Size(max = 100, message = "정답 단어는 100자를 넘을 수 없습니다.")
    val answerWord: String,

    @field:Schema(description = "힌트 (선택)", example = "미루다, 연기하다", nullable = true)
    @field:Size(max = 200, message = "힌트는 200자를 넘을 수 없습니다.")
    val hint: String? = null,

)


@Schema(description = "퀴즈 정보")
data class QuizResponse(

    @field:Schema(description = "퀴즈 ID", example = "1")
    val id: Long,

    @field:Schema(description = "퀴즈셋 ID", example = "1")
    val quizSetId: Long,

    @field:Schema(
        description = "빈칸 {{}} 이 포함된 영어 문장",
        example = "She decided to {{}} the meeting until next week.",
    )
    val sentence: String,

    @field:Schema(description = "정답 단어", example = "postpone")
    val answerWord: String,

    @field:Schema(description = "힌트 (선택)", example = "미루다, 연기하다", nullable = true)
    val hint: String?,

) {
    companion object {
        fun from(quiz: Quiz, quizSetId: Long): QuizResponse = QuizResponse(
            id = requireNotNull(quiz.id) {"저장되지 않은 Quiz는 응답으로 변환할 수 없습니다."},
            sentence = quiz.sentence,
            answerWord = quiz.answerWord,
            hint = quiz.hint,
            quizSetId = quizSetId
        )
    }
}
