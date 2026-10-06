package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.quiz.Quiz
import org.springframework.data.jpa.repository.JpaRepository

interface QuizRepository : JpaRepository<Quiz, Long> {

    fun findAllByQuizSetId(quizSetId: Long): List<Quiz>

    fun findAllByIdInAndQuizSetId(quizIds: List<Long>, quizSetId: Long): List<Quiz>
}