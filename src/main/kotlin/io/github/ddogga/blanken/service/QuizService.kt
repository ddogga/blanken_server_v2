package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Quiz
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizResponse
import io.github.ddogga.blanken.exception.QuizNotFoundException
import io.github.ddogga.blanken.exception.QuizSetNotFoundExceptions
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly  =true)
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
    fun changeQuizSet(quizId: Long, newQuizSetId: Long): QuizResponse {

        val quiz = findQuizById(quizId)

        val newQuizSet = findQuizSetById(newQuizSetId)

        quiz.updateQuizSet(newQuizSet)
        return QuizResponse.from(quiz, newQuizSetId)
    }


    fun findQuizSetById(quizSetId : Long): QuizSet {
        return quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundExceptions(quizSetId)
    }

    fun findQuizById(quizId : Long): Quiz {
        return quizRepository.findByIdOrNull(quizId)
            ?: throw QuizNotFoundException(quizId)
    }

}