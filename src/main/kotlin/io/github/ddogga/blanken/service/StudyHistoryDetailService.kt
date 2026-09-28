package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Quiz
import io.github.ddogga.blanken.domain.StudyHistory
import io.github.ddogga.blanken.domain.StudyHistoryDetail
import io.github.ddogga.blanken.dto.history.StudyHistoryDetailRequest
import io.github.ddogga.blanken.exception.QuizNotFoundException
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.StudyHistoryDetailRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class StudyHistoryDetailService(
    private val studyHistoryDetailRepository: StudyHistoryDetailRepository,
    private val quizRepository: QuizRepository
) {


    @Transactional
    fun create(request: List<StudyHistoryDetailRequest>, history: StudyHistory): Int {

        val historyDetails = request.map{
            req ->
            val detail = StudyHistoryDetail(
                quiz = findQuizById(req.quizId),
                gaveUp = req.gaveUp,
            )
            history.addDetail(detail)
            detail
        }
        return studyHistoryDetailRepository.saveAll(historyDetails).size
    }

    private fun findQuizById(quizId: Long): Quiz {
        return quizRepository.findByIdOrNull(quizId)
            ?: throw QuizNotFoundException(quizId)
    }

}