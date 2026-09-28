package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.StudyHistoryDetail
import org.springframework.data.jpa.repository.JpaRepository

interface StudyHistoryDetailRepository : JpaRepository<StudyHistoryDetail, Long> {
}