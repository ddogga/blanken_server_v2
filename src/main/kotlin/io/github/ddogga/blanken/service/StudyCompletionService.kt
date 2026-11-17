package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.exception.StudySessionMismatchException
import io.github.ddogga.blanken.exception.StudySessionNotCompletableException
import io.github.ddogga.blanken.exception.StudySessionNotFoundException
import io.github.ddogga.blanken.repository.StudySessionRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service


/**
 * self invocation 예방을 위한 StudyHistoryService와 빈 분리
 */


@Service
class StudyCompletionService(
    private val studyHistoryService: StudyHistoryService,
    private val sessionRepository: StudySessionRedisRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun complete(userId: Long, quizSetId: Long, sessionId: String): StudyHistoryResponse {

        // 1. 멱등: 이미 생성된 학습 히스토리 확인
        studyHistoryService.findRecordedBy(userId, quizSetId, sessionId)?.let {
            recorded -> deleteSessionQuietly(userId, quizSetId, sessionId) // 이전 요청에서 세션 삭제가 실패 했을 수 있으니 한번 더 진행
            return recorded
        }

        // 2. 세션 확인
        val session = sessionRepository.getAndExtendTtl(userId, quizSetId)
            ?: throw StudySessionNotFoundException(quizSetId)

        if (session.sessionId != sessionId)
            throw StudySessionMismatchException(sessionId)

        // 3. 완주 확인
        val unsolvedCount = session.quizzes.count { it.quizId !in session.results }
        if (unsolvedCount > 0) throw StudySessionNotCompletableException(sessionId, unsolvedCount)

        // 4. 학습 히스토리 생성
        val response = try {
            studyHistoryService.recordCompletion(userId, quizSetId, session)
        } catch (e: DataIntegrityViolationException) {
            recoverFromConflict(userId, quizSetId, sessionId, e)
        } catch (e: ObjectOptimisticLockingFailureException) {
            recoverFromConflict(userId, quizSetId, sessionId, e)
        }

        // 5. 세션 삭제
        deleteSessionQuietly(userId, quizSetId, sessionId)
        return response
    }

    /**
     * 동시에 들어온 다른 요청이 먼저 반영된 경우.
     * 같은 학습 세션이면 이전 요청 결과 유지, 아니라면 충돌이므로 예외 반환
     */
    private fun recoverFromConflict(
        userId: Long,
        quizSetId: Long,
        sessionId: String,
        cause: RuntimeException,
    ): StudyHistoryResponse =
        studyHistoryService.findRecordedBy(userId, quizSetId, sessionId)
            ?: throw cause

    /** 세션 삭제는 실패해도 완주 결과에 영향이 없기 때문에 로그로만 기록 (TTL로 정리됨)*/
    private fun deleteSessionQuietly(userId: Long, quizSetId: Long, sessionId: String) {
        try {
            sessionRepository.deleteIfSessionMatches(userId, quizSetId, sessionId)
        } catch (e: Exception) {
            log.warn("Failed to delete study session after completion. userId={}, quizSetId={}", userId, quizSetId, e)
        }
    }


}