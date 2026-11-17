package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.QuizRevision
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.history.StudyHistory
import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.dto.history.StudyCompletion
import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.dto.quiz.QuizRevisionResponse
import io.github.ddogga.blanken.dto.quiz.QuizRevisionRequest
import io.github.ddogga.blanken.dto.study.StudyResult
import io.github.ddogga.blanken.dto.study.StudySession
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
import java.time.Instant


@Service
@Transactional(readOnly = true)
class StudyHistoryService(
    private val studyHistoryRepository: StudyHistoryRepository,
    private val userRepository: UserRepository,
    private val quizSetRepository: QuizSetRepository,
    private val quizRevisionRepository: QuizRevisionRepository,
) {


    fun findRecordedBy(userId: Long, quizSetId: Long, sessionId: String): StudyHistoryResponse?
    = studyHistoryRepository.findByUserIdAndQuizSetId(userId, quizSetId)
        ?.takeIf { it.isRecordedBy(sessionId) }
        ?.let { StudyHistoryResponse.from(it) }



    @Transactional
    fun recordCompletion(userId: Long, quizSetId: Long, session: StudySession): StudyHistoryResponse {

        // 학습 기록 생성 여부 확인
        val existing = studyHistoryRepository.findByUserIdAndQuizSetId(userId, quizSetId)

        val completion = toCompletion(session, quizSetId, existing)

        val history = existing
            ?.also { it.recordCompletion(completion) }
            ?: StudyHistory(userId = userId, quizSetId = quizSetId, firstCompletion = completion)

        // saveAndFlush: 유니크 위반, 낙관적 락 예외를 커밋 시점이 아니라 이 메서드 안에서 드러나게 함.
        val saved = studyHistoryRepository.saveAndFlush(history)
        return StudyHistoryResponse.from(saved)
    }

    /** Redis 세션 -> Study History의 완주 정보 */
    private fun toCompletion(session: StudySession, quizSetId: Long, existing: StudyHistory?): StudyCompletion {

        // wrong revision
        val wrongRevisionIds = session.quizzes
            .filter { session.results[it.quizId] == StudyResult.GAVE_UP }
            .map { it.revisionId }

        val wrongRevisions = wrongRevisionIds.map {
            quizRevisionRepository.getReferenceById(it)
        }

        // 최신 정보로 저장
        val title = quizSetRepository.findTitleById(quizSetId)
            ?: existing?.quizSetTitle
            ?: throw QuizSetNotFoundException(quizSetId)

        val total = session.quizzes.size
        return StudyCompletion(
            sessionId = session.sessionId,
            quizSetTitle = title,
            totalCount = total,
            correctCount = total - wrongRevisions.size,
            wrongRevisions = wrongRevisions,
            completedAt = Instant.now(),
        )
    }



}