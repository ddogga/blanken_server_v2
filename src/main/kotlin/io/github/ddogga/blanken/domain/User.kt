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

/**
 *
 * 이메일은 유니크 제약 조건 x
 * 같은 이메일로 소셜 , 일반 회원 가입 가능
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

    @Column(name = "password")
    var password: String,

    @Column(name = "nickname")
    var nickname : String,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    var oauthProvider: OauthProvider,

    @Column(name = "provider_id")
    var providerId : String,

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

    ) : BaseTimeEntity()