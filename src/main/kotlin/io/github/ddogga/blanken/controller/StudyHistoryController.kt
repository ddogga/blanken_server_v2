package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.dto.history.StudyHistoryRequest
import io.github.ddogga.blanken.dto.history.StudyHistoryResponse
import io.github.ddogga.blanken.service.StudyHistoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI


@Tag(name = "StudyHistory", description = "학습 히스토리 관리 API")
@RestController
@RequestMapping("/api/study-histories")
class StudyHistoryController(
    private val studyHistoryService: StudyHistoryService
) {

    @Operation(summary = "학습 히스토리 생성", description = "학습 히스토리를 생성합니다.")
    @PostMapping
    fun create(@Valid @RequestBody request: StudyHistoryRequest)
        : ResponseEntity<StudyHistoryResponse> {
        val newStudyHistory = studyHistoryService.create(request)
        return ResponseEntity.created(URI.create("/api/study-histories/${newStudyHistory.id}")).body(newStudyHistory)
    }


}