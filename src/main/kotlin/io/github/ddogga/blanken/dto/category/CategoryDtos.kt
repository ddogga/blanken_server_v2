package io.github.ddogga.blanken.dto.category

import io.github.ddogga.blanken.domain.Category
import io.swagger.v3.oas.annotations.media.Schema


@Schema(description = "카테고리")
data class CategoryResponse(

	@field:Schema(description = "카테고리 ID", example = "1")
	val id: Long,

	@field:Schema(description = "카테고리 이름", example = "토익")
	val name: String,
) {
	companion object {
		fun from(category: Category): CategoryResponse = CategoryResponse(
			id = requireNotNull(category.id) { "저장되지 않은 Category 는 응답으로 변환할 수 없습니다." },
			name = category.name,
		)
	}
}
