package io.github.ddogga.blanken.domain.user

import io.github.ddogga.blanken.domain.BaseTimeEntity
import io.github.ddogga.blanken.domain.OauthProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 *
 * email 중복을 허용한다. 같은 이메일, 다른 제공자 회원가입을 허용.
 * 소셜 로그인/가입만 허용.
 *
 */

@Entity
@Table(name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "users_provider_provider_id",
            columnNames = ["provider", "provider_id"]
        )
        ]
    )
class User(

    @Column(name = "email", nullable = false, length = 255)
	var email: String,

    //필요시 추가
//    @Column(name = "phon_number", unique = true)
//    var phonNumber: String,

    @Column(name = "nickname")
    var nickname : String,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    var oauthProvider: OauthProvider?,

    @Column(name = "provider_id")
    var providerId : String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    var userStatus: UserStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    var userRole: UserRole,

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,

    ) : BaseTimeEntity() {


    @Column(name = "terms_agreed_at")
    var termsAgreedAt: Instant? = null
        protected set

    @Column(name = "marketing_agreed", nullable = false)
    var marketingAgreed: Boolean = false
        protected set

    @Column(name = "withdrawn_at") // 유저 탈퇴 시간 - TODO: 24시간 동안 재가입 막음
    var withdrawnAt: Instant? = null
        protected set


    fun completeSignup(
        nickname: String,
        marketingAgreed: Boolean,
        termsAgreedAt: Instant
    ) {
        this.nickname = nickname
        this.userRole = UserRole.USER
        this.userStatus = UserStatus.ACTIVE
        this.termsAgreedAt = termsAgreedAt
        this.marketingAgreed = marketingAgreed
    }

    fun deleteUser() {
        this.userStatus = UserStatus.WITHDRAWN
        this.withdrawnAt = Instant.now()
    }

}