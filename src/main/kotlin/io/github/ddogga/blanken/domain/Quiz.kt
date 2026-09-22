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

	init {
		validateSentence(sentence)
	}

	fun update(sentence: String, answerWord: String, hint: String?) {
		validateSentence(sentence)
		this.sentence = sentence
		this.answerWord = answerWord
		this.hint = hint
	}

    fun updateQuizSet(newQuizSet : QuizSet) {
        this.quizSet.removeQuiz(this)
        this.quizSet = newQuizSet
    }


	companion object {

		const val BLANK = "{{}}"
		const val SENTENCE_PATTERN = "^[^{}]*\\{\\{\\}\\}[^{}]*$"
		const val SENTENCE_RULE_MESSAGE = "문장에는 빈칸 {{}} 이 정확히 하나 있어야 합니다."

		private val sentenceRegex = Regex(SENTENCE_PATTERN)

		private fun validateSentence(sentence: String) {
			require(sentenceRegex.matches(sentence)) { SENTENCE_RULE_MESSAGE }
		}
	}
}
