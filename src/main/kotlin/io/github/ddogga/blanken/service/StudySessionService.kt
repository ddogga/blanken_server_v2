package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.quiz.Visibility
import io.github.ddogga.blanken.dto.study.StudySessionStartResult
import io.github.ddogga.blanken.exception.NotEnoughQuizzesException
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.StudySessionRedisRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID


@Service
@Transactional(readOnly = true)
class StudySessionService(
    private val quizSetRepository: QuizSetRepository,
    private val quizRepository: QuizRepository,
    private val sessionRepository: StudySessionRedisRepository,
) {

    // 학습 시작 : 진행중인 세션이 있으면 이어풀기, 없으면 새로 생성.
    fun start(userId: Long, quizSetId: Long) : StudySessionStartResult {
        val quizSet = findQuizSetById(quizSetId)

        checkQuizSetForStudy(quizSet, userId)

        sessionRepository.getAndExtendTtl(userId, quizSetId)?.let { existing ->
            return StudySessionStartResult(existing, created = false)
        }
        // 세션 없으면 생성
        val snapshots = quizRepository.findAllByQuizSetId(quizSetId).map { it.toSnapshot() }

        if (snapshots.size < QuizSet.MIN_STUDY_QUIZ_COUNT)
            throw NotEnoughQuizzesException(quizSetId, QuizSet.MIN_STUDY_QUIZ_COUNT, snapshots.size)

        return sessionRepository.createIfAbsent(
            userId = userId,
            quizSetId = quizSetId,
            sessionId = UUID.randomUUID().toString(),
            quizzes = snapshots,
            startedAt = Instant.now()
        )
    }


    /** 처음부터 다시 - 기존 세션을 버리고 새로 시작 */
    fun restart(userId: Long, quizSetId: Long): StudySessionStartResult {
        sessionRepository.delete(userId, quizSetId)
        return start(userId, quizSetId)
    }


    private fun checkQuizSetForStudy(quizSet: QuizSet, userId: Long) {
        val isOwner = quizSet.owner.id == userId
        if (quizSet.visibility == Visibility.PRIVATE && !isOwner) {
            // 비공개 퀴즈셋은 존재 여부도 숨기기
            throw QuizSetNotFoundException(quizSet.id!!)
        }
    }


    private fun findQuizSetById(quizSetId: Long): QuizSet {
        return quizSetRepository.findByIdOrNull(quizSetId)
            ?: throw QuizSetNotFoundException(quizSetId)
    }



}
