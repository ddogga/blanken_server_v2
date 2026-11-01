package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.QuizRevision
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.history.StudyHistory
import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.dto.history.StudyHistoryRequest
import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.dto.quiz.QuizRevisionResponse
import io.github.ddogga.blanken.dto.quiz.QuizRevisionRequest
import io.github.ddogga.blanken.exception.QuizRevisionNotFoundException
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.QuizRevisionRepository
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
    private val quizRevisionRepository: QuizRevisionRepository,
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
            userId = requireNotNull(user.id),
            quizSetId = requireNotNull(quizSet.id),
            quizSetTitle = quizSet.title,
            score = calculateScore(request.correctCount, request.totalCount),
            totalCount = request.totalCount,
            correctCount = request.correctCount,
            wrongQuizzes = findWrongRevisions(request.wrongRevisionIds),
        )

        val newHistory = studyHistoryRepository.save(studyHistory)

        return toResponse(newHistory)
    }

    /**
     * 세션이 들고 있던 리비전 id 로 오답 스냅샷을 찾는다.
     *
     * `findAllById` 는 없는 id 를 **에러 없이 결과에서 뺀다**. 그대로 두면 오답 일부가 조용히 누락되고
     * 요청의 개수 검증(오답 수 + 맞힌 수 = 푼 문제 수)과 저장된 기록이 어긋나므로, 하나라도 없으면 거부한다.
     */
    private fun findWrongRevisions(wrongRevisionIds: List<Long>): List<QuizRevision> {
        val revisions = quizRevisionRepository.findAllById(wrongRevisionIds)

        val missingIds = wrongRevisionIds.toSet() - revisions.map { requireNotNull(it.id) }.toSet()
        if (missingIds.isNotEmpty()) throw QuizRevisionNotFoundException(missingIds)

        return revisions
    }

    private fun toResponse(studyHistory: StudyHistory): StudyHistoryResponse = StudyHistoryResponse(
        id = requireNotNull(studyHistory.id) { "저장되지 않은 StudyHistory는 응답으로 변환할 수 없습니다." },
        userId = requireNotNull(studyHistory.userId) { "유저 정보가 없는 StudyHistory입니다." },
        quizSetId = requireNotNull(studyHistory.quizSetId) { "퀴즈셋 정보가 없는 StudyHistory입니다." },
        quizSetTitle = studyHistory.quizSetTitle,
        score = studyHistory.score,
        totalCount = studyHistory.totalCount,
        correctCount = studyHistory.correctCount,
        wrongQuizzes = studyHistory.wrongQuizzes.map(QuizRevisionResponse::from),
    )

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