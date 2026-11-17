package io.github.ddogga.blanken.domain.history

import io.github.ddogga.blanken.domain.quiz.QuizRevision
import io.github.ddogga.blanken.dto.history.StudyCompletion
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
import jakarta.persistence.Version
import java.math.BigDecimal
import java.math.RoundingMode
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

    /** val/var를 붙여야 클래스의 프로퍼티가 됨. 이건 없으므로 생성자에서만 쓰이는 인자 */
    firstCompletion: StudyCompletion,

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,
) {

    /** 완주 시점의 퀴즈셋 제목 */
    @Column(name = "quiz_set_title", nullable = false)
    var quizSetTitle: String = firstCompletion.quizSetTitle
        protected set

    /** 최근 점수 (0.00 ~ 100.00) */
    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    var score: BigDecimal = BigDecimal.ZERO
        protected set

    @Column(name = "best_score", nullable = false, precision = 5, scale = 2)
    var bestScore: BigDecimal = BigDecimal.ZERO
        protected set

    @Column(name = "total_count", nullable = false)
    var totalCount: Int = 0
        protected set

    @Column(name = "correct_count", nullable = false)
    var correctCount: Int = 0
        protected set

    /** 이 퀴즈셋 완주 횟수 */
    @Column(name = "completion_count", nullable = false)
    var completionCount: Int = 0
        protected set

    /** 최근 완주 시각 */
    @Column(name = "solved_at", nullable = false)
    var solvedAt: Instant = firstCompletion.completedAt
        protected set

    @Column(name = "last_session_id", nullable = false)
    var lastSessionId: String = firstCompletion.sessionId
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    /** 최근 완주에서 포기한 퀴즈의 리비전 (오답노트 대상) */
    @ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "study_history_wrong_revision",
		joinColumns = [JoinColumn(name = "history_id")],
		inverseJoinColumns = [JoinColumn(name = "revision_id")],
	)
	private val mutableWrongRevisions: MutableList<QuizRevision> = mutableListOf()

	val wrongQuizzes: List<QuizRevision> get() = mutableWrongRevisions

    init {
        // mutableWrongRevisions 선언 뒤에 있어야 함 — Kotlin은 선언 순서대로 초기화
        recordCompletion(firstCompletion)
    }

    /** 이 세션의 완주가 이미 반영됐는지 체크 */
    fun isRecordedBy(sessionId: String): Boolean = lastSessionId == sessionId

    /** 완주 결과를 반영 (덮어쓰기 + 누적값 갱신) */
    fun recordCompletion(completion: StudyCompletion) {
        val newScore = calculateScore(completion.correctCount, completion.totalCount)

        quizSetTitle = completion.quizSetTitle
        totalCount = completion.totalCount
        correctCount = completion.correctCount
        score = newScore
        bestScore = bestScore.max(newScore)
        completionCount += 1
        solvedAt = completion.completedAt
        lastSessionId = completion.sessionId

        // 오답 목록은 최신 회차로 교체
        mutableWrongRevisions.clear()
        mutableWrongRevisions.addAll(completion.wrongRevisions)
    }



    companion object {

        fun calculateScore(correctCount: Int, totalCount: Int): BigDecimal {
            require(totalCount > 0) { "totalCount는 양수여야 합니다."}
            require(correctCount in 0..totalCount) { "correctCount는 0부터 totalCount 사이의 수여야 합니다."}
            return BigDecimal(correctCount * 100)
                .divide(BigDecimal(totalCount), 2, RoundingMode.HALF_UP)
        }

    }



}
