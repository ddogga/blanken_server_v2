package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Quiz
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizResponse
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

        val quizSet = quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundExceptions(quizSetId)

        val quiz = Quiz(
            sentence = request.sentence,
            answerWord = request.answerWord,
            hint = request.hint
        )
        quizSet.addQuiz(quiz)
        val newQuiz = quizRepository.save(quiz)

        return QuizResponse.from(newQuiz)
    }
}