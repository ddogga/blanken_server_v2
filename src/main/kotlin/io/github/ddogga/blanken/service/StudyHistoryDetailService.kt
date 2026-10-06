package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.Quiz
import io.github.ddogga.blanken.domain.history.StudyHistory
import io.github.ddogga.blanken.domain.history.StudyHistoryDetail
import io.github.ddogga.blanken.dto.history.StudyHistoryDetailRequest
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.StudyHistoryDetailRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class StudyHistoryDetailService(
    private val studyHistoryDetailRepository: StudyHistoryDetailRepository,
    private val quizRepository: QuizRepository
) {


    @Transactional
    fun create(request: List<StudyHistoryDetailRequest>, history: StudyHistory, quizSetId: Long): Int {

        val quizIds = request.map { it.quizId }
        require(quizIds.size == quizIds.toSet().size) { "학습 세부사항 목록에 중복된 정보가 들어 있습니다." }
        val quizzes = quizRepository.findAllByIdInAndQuizSetId(quizIds, quizSetId)
        require(quizzes.size == quizIds.size) { "다른 퀴즈셋의 학습 정보가 들어 있습니다." }

        val quizMap: Map<Long, Quiz> = quizzes.associateBy { requireNotNull(it.id) }

        val historyDetails = request.map{
            req ->
            val detail = StudyHistoryDetail(
                quiz = quizMap.getValue(req.quizId),
                gaveUp = req.gaveUp,
            )
            history.addDetail(detail)
            detail
        }
        return historyDetails.size
    }



}