package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.QuizSetOrderEnum
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetUpdateRequest
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.QuizSetNotFoundExceptions
import io.github.ddogga.blanken.exception.QuizSetTitleDuplicationException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuizSetService(
    private val quizSetRepository: QuizSetRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
) {

    @Transactional
    fun create(request: QuizSetCreateRequest): QuizSetResponse {

        val owner = userRepository.findByIdOrNull(request.ownerId)
            ?: throw UserNotFoundException(request.ownerId)

        checkTitleDuplication(request.ownerId, request.title)

        val quizSet = QuizSet(
            owner = owner,
            category = findCategoryById(request.categoryId),
            title = request.title,
            description = request.description,
            visibility = request.visibility,
        )

        return QuizSetResponse.from(quizSetRepository.save(quizSet))
    }


    @Transactional
    fun update(quizSetId: Long, request: QuizSetUpdateRequest): QuizSetResponse {

        val quizSet = quizSetRepository.findWithCategoryById(quizSetId)
            ?: throw QuizSetNotFoundExceptions(quizSetId)

        checkTitleDuplication(quizSet.owner.id!!, request.title)

        quizSet.update(request)
        quizSet.updateCategory(findCategoryById(request.categoryId))

        return QuizSetResponse.from(quizSet)
    }

    fun search(
        keyword: String?,
        categoryId: Long?,
        orderEnum: QuizSetOrderEnum,
        pageable: Pageable,
    ): PageResponse<QuizSetResponse> =
        PageResponse.from(quizSetRepository.searchQuizSet(keyword, categoryId, orderEnum, pageable))



    private fun findCategoryById(categoryId: Long): Category =
        categoryRepository.findByIdOrNull(categoryId)
            ?: throw CategoryNotFoundException(categoryId)

    private fun checkTitleDuplication(ownerId : Long, title : String) {
        val isDuplicated = quizSetRepository.existsByOwnerIdAndTitle(ownerId, title)
        if (isDuplicated) {
            throw QuizSetTitleDuplicationException(title)
        }
    }
}
