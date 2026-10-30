package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.Category
import io.github.ddogga.blanken.domain.quiz.Quiz
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.quiz.QuizSetOrderEnum
import io.github.ddogga.blanken.domain.quiz.Visibility
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetDetailResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetUpdateRequest
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.QuizNotFoundException
import io.github.ddogga.blanken.exception.QuizSetAccessDeniedException
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
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
    fun update(quizSetId: Long, request: QuizSetUpdateRequest): QuizSetDetailResponse {

        val quizSet = quizSetRepository.findWithCategoryAndQuizzesById(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)

        if (quizSet.title != request.title) checkTitleDuplication(quizSet.owner.id!!, request.title)

        quizSet.update(request)
        quizSet.updateCategory(findCategoryById(request.categoryId))

        val existQuizMap = quizSet.quizzes.associateBy { it.id }

        // quiz delete
        val newQuizIds = request.quizzes.map { it.id }.toSet()
        quizSet.quizzes.forEach { it ->
            if (it.id !in newQuizIds) quizSet.removeQuiz(it)
        }

        // quiz update
        request.quizzes.forEachIndexed { index, dto ->
            if (dto.id != null) {
                (existQuizMap[dto.id] ?: throw QuizNotFoundException(dto.id))
                    .update(dto)
            } else {
                quizSet.addQuiz(Quiz(
                    sentence = dto.sentence,
                    answers = dto.answers,
                    hint = dto.hint,
                    orderIndex = dto.orderIndex,
                    translation = dto.translation,
                ))
            }
        }

        quizSetRepository.flush()

        return QuizSetDetailResponse.from(quizSet)
    }

    fun search(
        keyword: String?,
        categoryId: Long?,
        orderEnum: QuizSetOrderEnum,
        pageable: Pageable,
    ): PageResponse<QuizSetResponse> =
        PageResponse.from(quizSetRepository
            .searchQuizSet(keyword, categoryId, orderEnum, pageable))


    fun getQuizSetDetail(
        quizSetId: Long,
        requestUserId: Long
    ): QuizSetDetailResponse {
        val quizSet = quizSetRepository.findWithCategoryAndQuizzesById(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)

        if (quizSet.visibility == Visibility.PRIVATE && quizSet.owner.id != requestUserId) {
            throw QuizSetAccessDeniedException(quizSetId)
        }
        return QuizSetDetailResponse.from(quizSet)
    }





    fun getUserQuizSets(userId: Long, orderEnum: QuizSetOrderEnum, pageable: Pageable
    ): PageResponse<QuizSetResponse> =
        PageResponse.from(
            quizSetRepository.getUserQuizSets(userId, orderEnum, pageable)
        )

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
