package io.github.ddogga.blanken.dto.history

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

@Schema(description = "학습 히스토리 문제별 결과 생성 요청")
data class StudyHistoryDetailRequest(

    @field:Schema(description = "퀴즈 ID", example = "1")
    @field:Positive(message = "퀴즈 ID는 양수여야 합니다.")
    val quizId: Long,

    @field:Schema(description = "포기 여부 — true 면 오답으로 기록된다", example = "false")
    val gaveUp: Boolean = false,
)