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
import jakarta.persistence.UniqueConstraint

/**
 * 퀴즈셋 좋아요.
 *
 * UNIQUE(user_id, quiz_set_id) 가 중복 좋아요를 막는 유일한 수단이다.
 * 클라이언트는 낙관적 UI 로 동작하므로 더블탭·재시도로 중복 요청이 들어올 수 있는데,
 * 애플리케이션에서 "조회 후 없으면 삽입"(check-then-act) 으로 처리하면 동시 요청에서 경쟁이 발생한다.
 * 제약 위반을 DB 가 원자적으로 거부하게 두고, 서비스는 그 예외를 멱등하게 흡수한다.
 */
@Entity
@Table(
	name = "quiz_set_like",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_quiz_set_like_user_quiz_set", columnNames = ["user_id", "quiz_set_id"]),
	],
)
class QuizSetLike(

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	var user: User,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_set_id", nullable = false)
	var quizSet: QuizSet,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,

) : BaseTimeEntity()
