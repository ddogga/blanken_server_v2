package io.github.ddogga.blanken.dto.common

import io.swagger.v3.oas.annotations.media.Schema


@Schema(description = "커서 페이징 응답")
data class CursorResponse<T>(

    @field:Schema(description = "현재 페이지 내용")
    val content: List<T>,

    @field:Schema(description = "다음 커서 번호", example = "98")
    val nextCursor: Long?,

    @field:Schema(description = "true")
    val hasNext: Boolean,

) {
    companion object {
        fun<T : Any> from(content : List<T>, nextCursor: Long?, hasNext: Boolean): CursorResponse<T> = CursorResponse(
            content = content,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }
}