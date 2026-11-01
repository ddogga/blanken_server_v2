package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.quiz.QuizRevision
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface QuizRevisionRepository : JpaRepository<QuizRevision, Long> {

}
