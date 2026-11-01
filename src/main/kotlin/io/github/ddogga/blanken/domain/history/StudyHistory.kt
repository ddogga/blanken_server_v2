package io.github.ddogga.blanken.domain.history

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
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
