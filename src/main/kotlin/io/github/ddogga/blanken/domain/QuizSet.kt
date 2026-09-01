package io.github.ddogga.blanken.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "quiz_set")
class QuizSet private constructor(

    /** owner 정보는 QuizSet 생성 시점에 있어야 하므로 생성자 안*/
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	var owner: User,

	@Column(name = "title", nullable = false, length = 100)
	var title: String,

	@Column(name = "description", length = 500)
	var description: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 20)
	var visibility: Visibility = Visibility.PUBLIC,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,

) : BaseTimeEntity() {

	@Column(name = "like_count", nullable = false)
	var likeCount: Int = 0
		protected set

    @Column(name = "quiz_count", nullable = false)
    var quizCount: Int = 0
        protected set

	@OneToMany(mappedBy = "quizSet", cascade = [CascadeType.ALL], orphanRemoval = true)
	private val mutableQuizzes: MutableList<Quiz> = mutableListOf()

	val quizzes: List<Quiz> get() = mutableQuizzes

	@OneToMany(mappedBy = "quizSet", cascade = [CascadeType.ALL], orphanRemoval = true)
	private val mutableCategories: MutableList<QuizSetCategory> = mutableListOf()

	val categories: List<QuizSetCategory> get() = mutableCategories

	fun addQuiz(quiz: Quiz) {
		mutableQuizzes.add(quiz)
		quiz.quizSet = this
	}

	fun removeQuiz(quiz: Quiz) {
		mutableQuizzes.remove(quiz)
	}

	fun addCategory(category: Category) {
		if (mutableCategories.any { it.category.id == category.id }) return
		mutableCategories.add(QuizSetCategory(quizSet = this, category = category))
	}

	fun removeCategory(category: Category) {
		if (mutableCategories.none { it.category.id == category.id }) return
		require(mutableCategories.size > 1) { CATEGORY_REQUIRED_MESSAGE }
		mutableCategories.removeIf { it.category.id == category.id }
	}

	companion object {

		private const val CATEGORY_REQUIRED_MESSAGE = "퀴즈셋에는 카테고리가 최소 1개 있어야 합니다."

		/**
		 * 퀴즈셋 생성 진입점.
		 *
		 * 기본 생성자를 막고 이 팩터리만 열어 둔 이유는 **카테고리 1개 이상**이라는 규칙 때문이다.
		 * 카테고리는 `addCategory` 로 생성 후에 붙는 구조라 생성자 파라미터로는 강제할 수 없다.
		 *
		 * 여기서 던지는 `IllegalArgumentException` 은 사용자 입력이 아니라 **서버 코드의 실수**를 잡는 방어선이다.
		 * 사용자 입력은 그 앞의 `QuizSetCreateRequest.categoryIds` 검증(`@Size(min = 1)`)이 400 으로 거른다.
		 */
		fun create(
			owner: User,
			title: String,
			description: String? = null,
			visibility: Visibility = Visibility.PUBLIC,
			categories: List<Category>,
		): QuizSet {
			require(categories.isNotEmpty()) { CATEGORY_REQUIRED_MESSAGE }

			return QuizSet(
				owner = owner,
				title = title,
				description = description,
				visibility = visibility,
			).apply { categories.forEach { addCategory(it) } }
		}
	}
}
