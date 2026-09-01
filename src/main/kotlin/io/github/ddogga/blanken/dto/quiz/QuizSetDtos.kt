package io.github.ddogga.blanken.dto.quiz

import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.category.CategoryResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(description = "퀴즈셋 생성 요청")
data class QuizSetCreateRequest(


	@field:Schema(description = "소유자 유저 ID", example = "1")
	@field:Positive(message = "소유자 ID는 양수여야 합니다.")
	val ownerId: Long,

	@field:Schema(description = "제목", example = "new quiz set")
	@field:NotBlank(message = "제목은 필수 입니다.")
	@field:Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
	val title: String,

	@field:Schema(description = "설명", example = "토익 빈출 동사 30선")
	@field:Size(max = 500, message = "설명은 500자를 넘을 수 없습니다.")
	val description: String? = null,

	@field:Schema(description = "공개 범위", example = "PUBLIC")
	@field:NotNull(message = "공개 범위는 필수 입니다.")
	val visibility: Visibility = Visibility.PUBLIC,

	/**
	 * **최소 1개는 필수다.** 카테고리 없는 퀴즈셋은 카테고리 필터 검색에 영원히 잡히지 않는다.
	 *
	 * 기본값을 빈 목록으로 두는 이유는, 필드를 통째로 생략했을 때 Jackson 역직렬화가 아니라
	 * 검증 단계에서 걸려 `fieldErrors` 가 붙은 400 이 나가게 하기 위해서다.
	 */
	@field:Schema(description = "카테고리 ID 목록 (최소 1개)", example = "[1, 3]")
	@field:Size(min = 1, message = "카테고리는 최소 1개 이상 선택해야 합니다.")
	val categoryIds: List<Long> = emptyList(),
)

/**
 * 퀴즈셋 응답.
 *
 * **`from` 은 반드시 트랜잭션 안에서 호출한다.** `open-in-view: false` 이고
 * `owner`·`categories` 가 모두 LAZY 라, 컨트롤러에서 매핑하면 `LazyInitializationException` 이 난다.
 *
 * 목록 조회에서는 `categories` 가 퀴즈셋마다 추가 쿼리를 부르므로 조회 쿼리에서 fetch join 으로 함께 가져와야 한다.
 */
@Schema(description = "퀴즈셋 정보")
data class QuizSetResponse(

	@field:Schema(description = "퀴즈셋 ID", example = "1")
	val id: Long,

	@field:Schema(description = "소유자 유저 ID", example = "1")
	val ownerId: Long,

	@field:Schema(description = "소유자 닉네임", example = "blanken")
	val ownerNickname: String,

	@field:Schema(description = "제목", example = "new quiz set")
	val title: String,

	@field:Schema(description = "설명", example = "토익 빈출 동사 30선")
	val description: String?,

	@field:Schema(description = "공개 범위", example = "PUBLIC")
	val visibility: Visibility,

	/** 정합성 기준은 DB지만 평상시 값은 Redis 카운터다. Redis 도입 후에는 그 값으로 덮어써야 한다. */
	@field:Schema(description = "좋아요 수", example = "12")
	val likeCount: Int,

	/** 2단계 생성 플로우상 생성 직후에는 항상 0이다. */
	@field:Schema(description = "퀴즈 개수", example = "20")
	val quizCount: Int,

	@field:Schema(description = "카테고리 목록")
	val categories: List<CategoryResponse>,

	@field:Schema(description = "생성 시각")
	val createdAt: Instant,

	@field:Schema(description = "마지막 수정 시각")
	val updatedAt: Instant,
) {
	companion object {
		fun from(quizSet: QuizSet): QuizSetResponse = QuizSetResponse(
			id = requireNotNull(quizSet.id) { "저장되지 않은 QuizSet 은 응답으로 변환할 수 없습니다." },
			ownerId = requireNotNull(quizSet.owner.id) { "저장되지 않은 User 는 소유자로 변환할 수 없습니다." },
			ownerNickname = quizSet.owner.nickname,
			title = quizSet.title,
			description = quizSet.description,
			visibility = quizSet.visibility,
			likeCount = quizSet.likeCount,
			quizCount = quizSet.quizzes.size,
			categories = quizSet.categories.map { CategoryResponse.from(it.category) },
			createdAt = quizSet.createdAt,
			updatedAt = quizSet.updatedAt,
		)
	}
}
