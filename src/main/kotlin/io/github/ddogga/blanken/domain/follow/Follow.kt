package io.github.ddogga.blanken.domain.follow

import io.github.ddogga.blanken.domain.BaseTimeEntity
import io.github.ddogga.blanken.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "follow",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_follow_follower_followee",
            columnNames = ["follower_id", "followee_id"]
        )
    ]
)
class Follow(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Column(name = "follower_id", nullable = false)
    var follower: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Column(name = "followee_id", nullable = false)
    var followee: User,

    @Column(name = "notify_quiz_set_created", nullable = false)
    var notifyQuizSetCreated: Boolean = true,

    @Column(name = "notify_quiz_battle_opened", nullable = false)
    var notifyQuizBattleOpened: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    ) : BaseTimeEntity() {

    init {
        require(follower.id == null || follower.id != followee.id) {
            "자기 자신을 팔로우할 수 없습니다."
        }
    }

    fun updateNotification(quizSetCreated: Boolean, quizBattleOpened: Boolean) {
        notifyQuizSetCreated = quizSetCreated
        notifyQuizBattleOpened = quizBattleOpened
    }
}