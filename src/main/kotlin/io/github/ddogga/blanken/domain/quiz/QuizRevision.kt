package io.github.ddogga.blanken.domain.quiz

import io.github.ddogga.blanken.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 퀴즈 스냅샷. 퀴즈가 수정될 때마다 그 시점의 내용
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "quiz_revision")
class QuizRevision(

    @Column(name = "quiz_id")
    val quizId: Long?,

    @Column(name = "sentence", nullable = false, length = 500)
    val sentence: String,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answers", nullable = false, columnDefinition = "text[]")
    val answers: List<String>,

    @Column(name = "hint", length = 200)
    val hint: String?,

    @Column(name = "translation", nullable = false, length = 500)
    val translation: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

) {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant
        protected set
}
