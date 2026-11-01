package io.github.ddogga.blanken.domain.history

import io.github.ddogga.blanken.domain.quiz.QuizRevision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint


@Entity
@Table(name =
    "study_history_detail",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_study_history_detail_revision_history", columnNames = ["revision_id", "history_id"])
    ]
)
class StudyHistoryDetail(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "revision_id", nullable = false)
	val revision: QuizRevision,

    @Column(name = "gave_up", nullable = false)
	var gaveUp: Boolean = false,

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,
) {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "history_id", nullable = false)
	lateinit var history: StudyHistory
}
