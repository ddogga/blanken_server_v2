package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.QuizSetLike
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.dto.quiz.QuizSetLikeRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetLikeResponse
import io.github.ddogga.blanken.exception.QuizSetLikeDuplicationException
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.QuizSetLikeRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
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
    fun addLikeQuizSet(request: QuizSetLikeRequest): QuizSetLikeResponse {
        
        val quizSet = findQuizSetById(request.quizSetId)
        val user = findUserById(request.userId)

        val quizSetLike = QuizSetLike(
            user = user,
            quizSet = quizSet
        )

        return try {
            val save = quizSetLikeRepository.saveAndFlush(quizSetLike)
            quizSetRepository.addLikeCount(request.quizSetId)
            QuizSetLikeResponse.from(save)
        } catch (ex: DataIntegrityViolationException) {
            throw QuizSetLikeDuplicationException(request.quizSetId)
        }

    }
    
    
    private fun findQuizSetById(quizSetId: Long): QuizSet =
        quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)

    private fun findUserById(userId: Long): User =
        userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

}