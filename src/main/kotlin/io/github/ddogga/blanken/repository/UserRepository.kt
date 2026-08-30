package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

	fun existsByEmail(email: String): Boolean

	fun findByEmail(email: String): User?
}
