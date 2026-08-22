package io.github.ddogga.blanken.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	val id: Long? = null,

) : BaseTimeEntity()
