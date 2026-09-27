package io.github.ddogga.blanken.dto.quiz

import io.github.ddogga.blanken.domain.QuizSetLike
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "퀴즈셋 좋아요 요청")
data class QuizSetLikeRequest(

    @field:Schema(
        description = "좋아요를 누른 유저 ID",
        example = "1"
    )
    @field:NotNull(message = "좋아요를 누른 유저 ID는 필수 입니다.")
    val userId : Long,

    @field:Schema(
        description = "좋아요가 눌린 퀴즈셋 ID", example = "1"
    )
    @field:NotNull(message = "좋아요가 눌린 퀴즈셋 ID는 필수 입니다.")
    val quizSetId : Long

)


@Schema(description = "퀴즈셋 좋아요 정보")
data class QuizSetLikeResponse(

    @field:Schema(description = "좋아요를 누른 유저 ID")
    val userId : Long,

    @field:Schema(description = "좋아요를 누른 유저 닉네임")
    val userNickName : String,

    @field:Schema(description = "좋아요가 눌린 퀴즈셋 ID")
    val quizSetId : Long,

    @field:Schema(description = "좋아요가 눌린 퀴즈셋 이름")
    val title : String,

    @field:Schema(description = "퀴즈셋 좋아요 개수")
    val likeCount : Int,

    @field:Schema(description = "퀴즈셋 좋아요 ID")
    val quizSetLikeId : Long

) {
    companion object {
        fun from(quizSetLike : QuizSetLike) : QuizSetLikeResponse = QuizSetLikeResponse(
            userId = requireNotNull(quizSetLike.user.id) {"저장된 유저만 응답으로 변환 할 수 있습니다."},
            userNickName = quizSetLike.user.nickname,
            quizSetId = requireNotNull(quizSetLike.quizSet.id) {"저장된 퀴즈셋만 응답으로 변환 할 수 있습니다."},
            title = quizSetLike.quizSet.title,
            likeCount = quizSetLike.quizSet.likeCount,
            quizSetLikeId = requireNotNull(quizSetLike.id) {"저장된 퀴즈셋 좋아요만 응답으로 변환 할 수 있습니다."}
        )
    }
}