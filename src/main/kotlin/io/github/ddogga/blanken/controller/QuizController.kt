package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizResponse
import io.github.ddogga.blanken.service.QuizService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
        return ResponseEntity.created(URI.create("/api/quiz-sets/${quizSetId}/quizzes/${newQuiz.id}")).body(newQuiz)
    }

    @Operation(summary = "퀴즈 수정", description = "퀴즈를 수정합니다.")
    @PutMapping("/{quizId}")
    fun updateQuiz(
        @Parameter(description = "퀴즈를 수정할 퀴즈셋 ID", example = "1")
        @PathVariable quizSetId: Long,
        @Parameter(description = "퀴즈 ID", example = "1")
        @PathVariable quizId: Long,
        @Valid @RequestBody request: QuizRequest
    ): QuizResponse = quizService.update(quizId, quizSetId, request)


    @Operation(summary = "퀴즈 여러개를 다른 퀴즈셋으로 옮기기", description = "퀴즈 여러개의 소속 퀴즈셋을 변경합니다.")
    @PatchMapping
    fun changeQuizSet(
        @Parameter(description = "옮길 quizSetId", example = "1")
        @PathVariable quizSetId: Long,
        @RequestParam(required = true) quizIds: List<Long>
    ): List<QuizResponse> = quizService.changeQuizSet(quizIds, quizSetId)


    @Operation(summary = "퀴즈셋 리스트 조회", description = "quizSetId로 소속 퀴즈 리스트를 조회합니다.")
    @GetMapping
    fun getQuizList(
        @Parameter(description = "quizSetId", example = "1")
        @PathVariable quizSetId: Long,
    ): List<QuizResponse> = quizService.getQuizList(quizSetId)


}