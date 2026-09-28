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
import java.math.BigDecimal
import java.math.RoundingMode


@Service
@Transactional(readOnly = true)
class StudyHistoryService(
    private val studyHistoryRepository: StudyHistoryRepository,
    private val userRepository: UserRepository,
    private val quizSetRepository: QuizSetRepository,
    private val studyHistoryDetailService: StudyHistoryDetailService
) {

    /**
     * TODO : 클라이언트 불안정으로 인한 중복 요청시 중복된 히스토리를 생성할 여지가 있음. 따라서 학습 세션키 필요 -> Redis 세션 시작시 세션 id(study:session:{userId}:{quizSetId}) 발급
     *
     */
    @Transactional
    fun create(request: StudyHistoryRequest): StudyHistoryResponse {

        val user = findUserById(request.userId)
        val quizSet = findQuizSetById(request.quizSetId)

        require(request.totalCount == quizSet.quizCount) { "완주한 퀴즈셋만 히스토리 저장이 가능합니다." }

        val studyHistory = StudyHistory(
            user = user,
            quizSet = quizSet,
            score = calculateScore(request.correctCount, request.totalCount),
            totalCount = request.totalCount,
            correctCount = request.correctCount
        )

        val newHistory = studyHistoryRepository.save(studyHistory)
        studyHistoryDetailService.create(request.details, newHistory, request.quizSetId)

        return StudyHistoryResponse.from(newHistory)
    }

    private fun calculateScore(correctCount: Int, totalCount: Int): BigDecimal{

        require(totalCount > 0) { "전체 문제 수는 0보다 커야 합니다."}
        require(totalCount >= correctCount) { "맞은 문제 수가 푼 문제 수 보다 클 수 없습니다."}

        return correctCount.toBigDecimal()
            .multiply(BigDecimal(100))
            .divide(totalCount.toBigDecimal(), 2, RoundingMode.HALF_UP)
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