package io.github.ddogga.blanken.dto.quiz

import io.github.ddogga.blanken.domain.Quiz
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "퀴즈 요청")
data class QuizRequest(

    /**
     * 수정 API 는 퀴즈 목록을 부분 변경(add/remove 요청)이 아니라 최종 상태 전체로 받는다.
     *
     * | 요청의 id | 기존 목록 | 처리 |
     * |---|---|---|
     * | `null` | — | INSERT |
     * | 있음 | 있음 | UPDATE |
     * | — | 요청에 없음 | DELETE |
     *
     * **주의**: 값이 있다고 곧바로 UPDATE 하면 안 된다. 다른 퀴즈셋의 퀴즈 id 를 보내는 요청을 막아야 하므로,
     * 서비스에서 **해당 퀴즈셋에 속한 id 인지 반드시 검증**한다.
     */
    @field:Schema(
        description = "퀴즈 ID. 기존 퀴즈는 그 값을, 새로 추가하는 퀴즈는 null 을 보낸다.",
        example = "1",
        nullable = true,
    )
    val id: Long?,

    /** 정답 단어를 **포함한** 완전한 문장. 빈칸 처리는 클라이언트가 `answerWord` 를 찾아 가린다. */
    @field:Schema(
        description = "정답 단어가 포함된 영어 문장",
        example = "She decided to postpone the meeting until next week.",
    )
    @field:NotBlank(message = "문장은 필수 입니다.")
    @field:Size(max = 500, message = "문장은 500자를 넘을 수 없습니다.")
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

    @field:Schema(
        description = "정답 단어가 포함된 영어 문장",
        example = "She decided to postpone the meeting until next week.",
    )
    val sentence: String,

    @field:Schema(description = "정답 단어", example = "postpone")
    val answerWord: String,

    @field:Schema(description = "힌트 (선택)", example = "미루다, 연기하다", nullable = true)
    val hint: String?,

) {
    companion object {
        fun from(quiz: Quiz): QuizResponse = QuizResponse(
            id = requireNotNull(quiz.id) {"저장되지 않은 Quiz는 응답으로 변환할 수 없습니다."},
            sentence = quiz.sentence,
            answerWord = quiz.answerWord,
            hint = quiz.hint,
        )
    }
}
