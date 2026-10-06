package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.Quiz
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.quiz.Visibility
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizResponse
import io.github.ddogga.blanken.exception.QuizNotFoundException
import io.github.ddogga.blanken.exception.QuizSetAccessDeniedException
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class QuizService (
    private val quizSetRepository: QuizSetRepository,
    private val quizRepository: QuizRepository,
) {

    @Transactional
    fun create(quizSetId: Long, request: QuizRequest): QuizResponse {

        val quizSet = findQuizSetById(quizSetId)
        val quiz = Quiz(
            sentence = request.sentence,
            answerWord = request.answerWord,
            hint = request.hint
        )
        quizSet.addQuiz(quiz)
        val newQuiz = quizRepository.save(quiz)

        return QuizResponse.from(newQuiz, quizSetId)
    }


    @Transactional
    fun update(quizId: Long, quizSetId: Long, request: QuizRequest): QuizResponse {

        val quiz = findQuizById(quizId)

        quiz.update(request.sentence, request.answerWord, request.hint)

        return QuizResponse.from(quiz, quizSetId)
    }


    @Transactional
    fun changeQuizSet(quizIds: List<Long>, newQuizSetId: Long): List<QuizResponse> {

        val newQuizSet = findQuizSetById(newQuizSetId)

        return quizIds.map { quizId ->
            val quiz = findQuizById(quizId)
            quiz.updateQuizSet(newQuizSet)
            QuizResponse.from(quiz, newQuizSetId)
        }

    }

    fun getQuizList(quizSetId: Long): List<QuizResponse> {

        val quizSet = findQuizSetById(quizSetId)
        if (quizSet.visibility != Visibility.PUBLIC) {
            throw QuizSetAccessDeniedException(quizSetId)
        }
        return quizRepository.findAllByQuizSetId(quizSetId).map{quiz ->
            QuizResponse.from(quiz, quizSetId)
        }

    }



    private fun findQuizSetById(quizSetId : Long): QuizSet {
        return quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)
    }

    private fun findQuizById(quizId : Long): Quiz {
        return quizRepository.findByIdOrNull(quizId)
            ?: throw QuizNotFoundException(quizId)
    }

}