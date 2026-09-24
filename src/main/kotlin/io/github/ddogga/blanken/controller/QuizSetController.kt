package io.github.ddogga.blanken.controller

import io.github.ddogga.blanken.domain.QuizSetOrderEnum
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetUpdateRequest
import io.github.ddogga.blanken.service.QuizSetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.net.URI


@Tag(name = "QuizSet", description = "퀴즈셋 관리 API")
@RestController
@RequestMapping("/api/quiz-sets")
class QuizSetController(
    private val quizSetService: QuizSetService,
) {

    @Operation(summary = "퀴즈셋 생성", description = "퀴즈셋을 생성합니다.")
    @PostMapping
    fun create(@Valid @RequestBody request: QuizSetCreateRequest): ResponseEntity<QuizSetResponse>{
        val newQuizSet = quizSetService.create(request)
        return ResponseEntity.created(URI.create("/api/quiz-sets/${newQuizSet.id}")).body(newQuizSet)
    }

    @Operation(summary = "퀴즈셋 수정", description = "퀴즈셋을 수정합니다.")
    @PutMapping("/{quizSetId}")
    fun update(
        @Parameter(description = "퀴즈셋 ID", example = "1")
        @PathVariable quizSetId: Long,
        @Valid @RequestBody request: QuizSetUpdateRequest
    ): QuizSetResponse = quizSetService.update(quizSetId, request)

    @Operation(summary = "퀴즈셋 검색", description = "퀴즈셋을 키워드, 카테고리로 검색합니다.")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = true) orderEnum: QuizSetOrderEnum,
        @PageableDefault(size = 20)
        pageable: Pageable,
    ): PageResponse<QuizSetResponse> =
        quizSetService.search(keyword, categoryId, orderEnum, pageable)


}