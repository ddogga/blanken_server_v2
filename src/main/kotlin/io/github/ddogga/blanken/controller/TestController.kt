package io.github.ddogga.blanken.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Schema(description = "테스트 응답")
data class TestResponse(
	@field:Schema(description = "응답 메시지", example = "Hello, blanken!")
	val message: String,
	@field:Schema(description = "응답 생성 시각")
	val timestamp: Instant = Instant.now(),
)

@Schema(description = "테스트 요청")
data class EchoRequest(
	@field:Schema(description = "그대로 돌려받을 문자열", example = "ping")
	val content: String,
)

@Tag(name = "Test", description = "Swagger 동작 확인용 테스트 API")
@RestController
@RequestMapping("/api/test")
class TestController {

	@Operation(summary = "핑 테스트", description = "서버가 살아있는지 확인합니다.")
	@GetMapping("/ping")
	fun ping(
		@Parameter(description = "인사할 대상 이름", example = "blanken")
		@RequestParam(defaultValue = "blanken") name: String,
	): TestResponse = TestResponse(message = "Hello, $name!")

	@Operation(summary = "에코 테스트", description = "요청 본문의 문자열을 그대로 돌려줍니다.")
	@PostMapping("/echo")
	fun echo(@RequestBody request: EchoRequest): TestResponse =
		TestResponse(message = request.content)
}
