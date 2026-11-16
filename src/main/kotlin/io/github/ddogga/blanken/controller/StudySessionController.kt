package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.dto.auth.AuthUser
import io.github.ddogga.blanken.dto.study.StudySessionProgressResponse
import io.github.ddogga.blanken.dto.study.StudySessionResponse
import io.github.ddogga.blanken.dto.study.StudySessionResultsRequest
import io.github.ddogga.blanken.service.StudySessionService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Study Session", description = "학습 세션을 관리하는 API")
@RestController
@RequestMapping("/api/quiz-sets/{quizSetId}/study-sessions")
class StudySessionController(
    private val studySessionService: StudySessionService
) {

    @PostMapping
    fun start(
        @AuthenticationPrincipal authUser: AuthUser,
        @Parameter(description = "퀴즈셋 ID", example = "1")
        @PathVariable quizSetId: Long,
        @RequestParam(defaultValue = "false") restart: Boolean,
    ): ResponseEntity<StudySessionResponse> {
        val result = if (restart) {
            studySessionService.restart(authUser.userId, quizSetId)
        } else {
            studySessionService.start(authUser.userId, quizSetId)
        }
        val status = if (result.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(StudySessionResponse.from(result))
    }

    @PatchMapping("/{sessionId}/results")
    fun saveResults(
        @AuthenticationPrincipal authUser: AuthUser,
        @PathVariable quizSetId: Long,
        @PathVariable sessionId: String,
        @Valid @RequestBody request: StudySessionResultsRequest,
    ): StudySessionProgressResponse =
        studySessionService.saveResults(authUser.userId, quizSetId, sessionId, request)


}