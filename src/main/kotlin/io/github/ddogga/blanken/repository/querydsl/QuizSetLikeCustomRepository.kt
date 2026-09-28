package io.github.ddogga.blanken.repository.querydsl

import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface QuizSetLikeCustomRepository {

    fun getLikeQuizSets(
        userId: Long,
        pageable: Pageable
    ): Page<QuizSetResponse>
}