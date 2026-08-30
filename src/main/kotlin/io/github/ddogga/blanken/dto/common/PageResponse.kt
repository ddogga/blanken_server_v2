package io.github.ddogga.blanken.dto.common

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

/**
 * 페이징 응답 래퍼.
 *
 * `Page`(`PageImpl`)를 그대로 직렬화하면 JSON 구조가 Spring Data 내부 구현에 묶여
 * 버전 간 안정성이 보장되지 않는다. 안드로이드 클라이언트가 고정된 형태로 파싱할 수 있도록 직접 감싼다.
 */
@Schema(description = "페이징 응답")
data class PageResponse<T>(

	@field:Schema(description = "현재 페이지 내용")
	val content: List<T>,

	@field:Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
	val page: Int,

	@field:Schema(description = "페이지 크기", example = "20")
	val size: Int,

	@field:Schema(description = "전체 요소 수", example = "137")
	val totalElements: Long,

	@field:Schema(description = "전체 페이지 수", example = "7")
	val totalPages: Int,

	@field:Schema(description = "첫 페이지 여부")
	val first: Boolean,

	@field:Schema(description = "마지막 페이지 여부")
	val last: Boolean,
) {
	companion object {
		fun <E : Any, T> from(page: Page<E>, mapper: (E) -> T): PageResponse<T> = PageResponse(
			content = page.content.map(mapper),
			page = page.number,
			size = page.size,
			totalElements = page.totalElements,
			totalPages = page.totalPages,
			first = page.isFirst,
			last = page.isLast,
		)
	}
}
