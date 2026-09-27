package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.QuizSetLike
import org.springframework.data.jpa.repository.JpaRepository


interface QuizSetLikeRepository : JpaRepository<QuizSetLike, Long>{

    fun deleteByUserIdAndQuizSetId(userId: Long, quizSetId: Long): Int


}