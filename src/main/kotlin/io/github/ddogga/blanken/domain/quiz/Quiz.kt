package io.github.ddogga.blanken.domain.quiz

import io.github.ddogga.blanken.domain.BaseTimeEntity
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes


@Entity
@Table(name = "quiz")
class Quiz(

	@Column(name = "sentence", nullable = false, length = 500)
	var sentence: String,

	answers: List<String>,

	@Column(name = "hint", length = 200)
	var hint: String? = null,

    @Column(name = "order_index")
    var orderIndex: Int, // 0부터 카운트

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,

) : BaseTimeEntity() {

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "answers", nullable = false, columnDefinition = "text[]")
	var answers: List<String> = answers
		protected set

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_set_id", nullable = false)
	lateinit var quizSet: QuizSet

	init {
		validate(sentence, answers)
	}

	fun update(newQuiz: QuizRequest) {
		validate(newQuiz.sentence, newQuiz.answers)
		this.sentence = newQuiz.sentence
		this.answers = newQuiz.answers
		this.hint = newQuiz.hint
        this.orderIndex = newQuiz.orderIndex
	}

    fun updateQuizSet(newQuizSet : QuizSet) {
        this.quizSet.removeQuiz(this)
        this.quizSet = newQuizSet
        newQuizSet.addQuiz(this)
    }


	companion object {

		const val BLANK = "{{}}"

		/** 빈칸이 하나 이상이여야 함. {{}} -> 빈칸 표시 */
		const val SENTENCE_PATTERN = "^[^{}]*(\\{\\{\\}\\}[^{}]*)+$"
		const val SENTENCE_RULE_MESSAGE = "문장에는 빈칸 {{}} 이 하나 이상 있어야 합니다."
		const val ANSWER_COUNT_RULE_MESSAGE = "정답 개수는 문장의 단어 수보다 적어야 합니다."
		const val ANSWER_WORD_RULE_MESSAGE = "정답에는 공백이 들어갈 수 없습니다."

		private val sentenceRegex = Regex(SENTENCE_PATTERN)
		private val whitespaceRegex = Regex("\\s+")

		/**
		 * - 문장: 빈칸이 하나 이상, 빈칸 외의 중괄호 없음
		 * - 정답: 각각 공백이 없을 것
		 * - 정답 개수: 공백으로 나눈 문장의 단어 수보다 반드시 적을 것
		 */
		private fun validate(sentence: String, answers: List<String>) {
			require(sentenceRegex.matches(sentence)) { SENTENCE_RULE_MESSAGE }
			require(answers.none { whitespaceRegex.containsMatchIn(it) }) { ANSWER_WORD_RULE_MESSAGE }
			require(answers.size < wordCount(sentence)) { ANSWER_COUNT_RULE_MESSAGE }
		}

		private fun wordCount(sentence: String): Int =
			sentence.trim().split(whitespaceRegex).count { it.isNotEmpty() }
	}
}
