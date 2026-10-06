package io.github.ddogga.blanken.domain.history

import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.domain.quiz.QuizSet
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant


@Entity
@Table(name = "study_history")
class StudyHistory(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	var user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_set_id", nullable = false)
	var quizSet: QuizSet,

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

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,
) {

	@OneToMany(mappedBy = "history", cascade = [CascadeType.ALL], orphanRemoval = true)
	private val historyDetails: MutableList<StudyHistoryDetail> = mutableListOf()

	val details: List<StudyHistoryDetail> get() = historyDetails

	fun addDetail(detail: StudyHistoryDetail) {
        historyDetails.add(detail)
		detail.history = this
	}
}
