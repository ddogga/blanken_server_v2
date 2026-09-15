package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.Quiz
import org.springframework.data.jpa.repository.JpaRepository

interface QuizRepository : JpaRepository<Quiz, Long> {
}