package io.github.ddogga.blanken.domain

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
 * 이메일은 유니크 설정
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

    @Column(name = "email", nullable = false, length = 255, unique = true)
	var email: String,

    //필요시 추가
//    @Column(name = "phon_number", unique = true)
//    var phonNumber: String,

    @Column(name = "password")
    var password: String?,

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



}