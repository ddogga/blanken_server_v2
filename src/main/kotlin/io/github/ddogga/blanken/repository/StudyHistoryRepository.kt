package io.github.ddogga.blanken.repository

import io.github.ddogga.blanken.domain.StudyHistory
import org.springframework.data.jpa.repository.JpaRepository

interface StudyHistoryRepository : JpaRepository<StudyHistory, Long>{

}