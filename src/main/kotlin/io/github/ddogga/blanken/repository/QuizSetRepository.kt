package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.QuizSet
import org.springframework.data.jpa.repository.JpaRepository

interface QuizSetRepository : JpaRepository<QuizSet, Long>{

}