package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.QuizSetLike
import io.github.ddogga.blanken.repository.querydsl.QuizSetLikeCustomRepository
import org.springframework.data.jpa.repository.JpaRepository


interface QuizSetLikeRepository : JpaRepository<QuizSetLike, Long>, QuizSetLikeCustomRepository{

    fun deleteByUserIdAndQuizSetId(userId: Long, quizSetId: Long): Int


}