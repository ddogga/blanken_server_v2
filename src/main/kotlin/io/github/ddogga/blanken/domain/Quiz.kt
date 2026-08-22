package io.github.ddogga.blanken.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "quiz")
class Quiz(

	@Column(name = "sentence", nullable = false, length = 500)
	var sentence: String,

	@Column(name = "answer_word", nullable = false, length = 100)
	var answerWord: String,

	@Column(name = "hint", length = 200)
	var hint: String? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,
) {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_set_id", nullable = false)
	lateinit var quizSet: QuizSet
}
