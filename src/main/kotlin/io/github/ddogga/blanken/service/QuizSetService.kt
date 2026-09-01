package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
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

        val foundCategories = findCategoriesByIds(request.categoryIds)

        val quizSet = QuizSet.create(
            owner = owner,
            title = request.title,
            description = request.description,
            visibility = request.visibility,
            categories = foundCategories,
        )

        // QuizSetCategory의 cascade 전략이 ALL이기 때문에 별도의 save 호출 없어도 자동 insert
        return QuizSetResponse.from(quizSetRepository.save(quizSet))
    }


    private fun findCategoriesByIds(categoryIds : List<Long>) : List<Category>{
        val requestedIds = categoryIds.toSet()
        val categories = categoryRepository.findAllById(requestedIds)
        val foundIds = categories.mapNotNull { it.id }.toSet()

        val missingIds = requestedIds - foundIds
        if (missingIds.isNotEmpty()) {
            throw CategoryNotFoundException(missingIds.toList())
        }

        return categories
    }
}
