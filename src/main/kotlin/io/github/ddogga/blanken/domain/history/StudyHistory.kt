package io.github.ddogga.blanken.domain.history

import io.github.ddogga.blanken.domain.quiz.QuizRevision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant


@Entity
@Table(name =
    "study_history",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_study_history_user_quiz_set", columnNames = ["user_id", "quiz_set_id"])
    ]
)
class StudyHistory(

    @Column(name = "user_id")
	val userId: Long?,

    @Column(name = "quiz_set_id")
	val quizSetId: Long?,

    @Column(name = "quiz_set_title")
    val quizSetTitle: String,

    @Column(name = "score", nullable = false)
	var score: BigDecimal,

    @Column(name = "total_count", nullable = false)
	var totalCount: Int,

    @Column(name = "correct_count", nullable = false)
	var correctCount: Int,

    @Column(name = "solved_at", nullable = false)
	var solvedAt: Instant = Instant.now(),

    // Redis 세션 추가시 추가
//    @Column(name = "session_id", nullable = false, unique = true)
//    var sessionId: String,

    wrongQuizzes: List<QuizRevision> = emptyList(),

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,
) {

	/**
	 * 오답(포기한) 퀴즈 목록.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "study_history_wrong_revision",
		joinColumns = [JoinColumn(name = "history_id")],
		inverseJoinColumns = [JoinColumn(name = "revision_id")],
	)
	private val mutableWrongRevisions: MutableList<QuizRevision> = wrongQuizzes.toMutableList()

	val wrongQuizzes: List<QuizRevision> get() = mutableWrongRevisions
}
