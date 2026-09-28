package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.StudyHistory
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.dto.history.StudyHistoryRequest
import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.StudyHistoryRepository
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class StudyHistoryService(
    private val studyHistoryRepository: StudyHistoryRepository,
    private val userRepository: UserRepository,
    private val quizSetRepository: QuizSetRepository,
    private val studyHistoryDetailService: StudyHistoryDetailService
) {

    @Transactional
    fun create(request: StudyHistoryRequest): StudyHistoryResponse {

        val user = findUserById(request.userId)
        val quizSet = findQuizSetById(request.quizSetId)

        val studyHistory = StudyHistory(
            user = user,
            quizSet = quizSet,
            score = request.score,
            totalCount = request.totalCount,
            correctCount = request.correctCount
        )

        val newHistory = studyHistoryRepository.save(studyHistory)
        studyHistoryDetailService.create(request.details, newHistory)

        return StudyHistoryResponse.from(newHistory)
    }


    private fun findUserById(userId: Long): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)
    }

    private fun findQuizSetById(quizSetId: Long): QuizSet {
        return quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)
    }


}