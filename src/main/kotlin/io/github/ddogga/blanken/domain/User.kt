package io.github.ddogga.blanken.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table


@Entity
@Table(name = "users")
class User(

	@Column(name = "email", nullable = false, unique = true, length = 255)
	var email: String,

	@Column(name = "password", nullable = false, length = 255)
	var password: String,

	@Column(name = "nickname", nullable = false, length = 50)
	var nickname: String,

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
