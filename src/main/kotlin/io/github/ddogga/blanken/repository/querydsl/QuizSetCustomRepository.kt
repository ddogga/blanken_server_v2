package io.github.ddogga.blanken.repository.querydsl

import io.github.ddogga.blanken.domain.QuizSetOrderEnum
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface QuizSetCustomRepository {

    fun searchQuizSet(
        keyword: String?,
        categoryId: Long?,
        orderEnum: QuizSetOrderEnum,
        pageable: Pageable
    ): Page<QuizSetResponse>
}