package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.QuizSetLike
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.github.ddogga.blanken.exception.QuizSetLikeDuplicationException
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.QuizSetLikeRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class QuizSetLikeService(
    private val quizSetLikeRepository: QuizSetLikeRepository,
    private val quizSetRepository: QuizSetRepository,
    private val userRepository: UserRepository
) {


    @Transactional
    fun addLikeQuizSet(quizSetId: Long, userId: Long): QuizSetResponse {
        
        val quizSet = findQuizSetById(quizSetId)
        val user = findUserById(userId)

        val quizSetLike = QuizSetLike(
            user = user,
            quizSet = quizSet
        )

        return try {
            quizSetLikeRepository.saveAndFlush(quizSetLike)
            quizSetRepository.addLikeCount(quizSetId)
            val update = findQuizSetById(quizSetId)
            QuizSetResponse.from(update)
        } catch (ex: DataIntegrityViolationException) {
            throw QuizSetLikeDuplicationException(quizSetId)
        }

    }

    @Transactional
    fun cancelLikeQuizSet(quizSetId: Long, userId: Long): QuizSetResponse {

        findQuizSetById(quizSetId)
        findUserById(userId)

        if (quizSetLikeRepository.deleteByUserIdAndQuizSetId(userId, quizSetId) == 1) {
            quizSetRepository.cancelLikeCount(quizSetId)
        }

        return QuizSetResponse.from(findQuizSetById(quizSetId))
    }

    fun getLikeQuizSets(userId: Long, pageable: Pageable): PageResponse<QuizSetResponse> =
        PageResponse.from(quizSetLikeRepository
            .getLikeQuizSets(userId, pageable))

    private fun findQuizSetById(quizSetId: Long): QuizSet =
        quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)

    private fun findUserById(userId: Long): User =
        userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

}