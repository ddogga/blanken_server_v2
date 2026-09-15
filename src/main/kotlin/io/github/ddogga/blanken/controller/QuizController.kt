package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizResponse
import io.github.ddogga.blanken.service.QuizService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI


@Tag(name = "Quiz", description = "퀴즈 관리 API")
@RestController
@RequestMapping("/api/quiz-sets/{quizSetId}/quizzes")
class QuizController (
    private val quizService: QuizService
){

    @Operation(summary = "퀴즈 추가", description = "퀴즈를 퀴즈셋에 추가 합니다.")
    @PostMapping
    fun addQuiz(
        @Parameter(description = "퀴즈를 추가할 퀴즈셋 ID", example = "1")
        @PathVariable quizSetId: Long,
        @Valid @RequestBody request: QuizRequest
    ): ResponseEntity<QuizResponse>{
        val newQuiz = quizService.create(quizSetId, request)
        return ResponseEntity.created(URI.create("/api/quiz-sets/{quizSetId}/quizzes/${newQuiz.id}")).body(newQuiz)
    }


}