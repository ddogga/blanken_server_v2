package io.github.ddogga.blanken.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.io.Serializable


@Entity
@Table(name = "quiz_set_category")
class QuizSetCategory(

	@MapsId("quizSetId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_set_id", nullable = false)
	var quizSet: QuizSet,

	@MapsId("categoryId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	var category: Category,
) {

	@EmbeddedId
	var id: QuizSetCategoryId = QuizSetCategoryId()
		protected set
}

/**
 * (quiz_set_id, category_id) 복합 기본키.
 * 같은 카테고리가 한 퀴즈셋에 두 번 붙는 것을 DB 차원에서 막는다.
 */
@Embeddable
class QuizSetCategoryId(

	@Column(name = "quiz_set_id")
	var quizSetId: Long = 0,

	@Column(name = "category_id")
	var categoryId: Long = 0,

) : Serializable {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is QuizSetCategoryId) return false
		return quizSetId == other.quizSetId && categoryId == other.categoryId
	}

	override fun hashCode(): Int = 31 * quizSetId.hashCode() + categoryId.hashCode()
}
