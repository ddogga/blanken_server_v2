package io.github.ddogga.blanken.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.ResponseEntity

/**
 * 공통 에러 응답.
 * 응답을 만들 때는 직접 생성자를 부르지 말고 [of] 를 쓴다.
 */
@Schema(description = "에러 응답")
data class ErrorResponse(

	@field:Schema(description = "에러 코드", example = "U001")
	val code: String,

	@field:Schema(description = "에러 메시지", example = "유저를 찾을 수 없습니다.")
	val message: String,

	@field:Schema(description = "필드별 검증 실패 내역 (검증 실패 시에만 존재)")
	val fieldErrors: List<FieldError>? = null,
) {
	@Schema(description = "필드 검증 실패")
	data class FieldError(

		@field:Schema(description = "필드명", example = "email")
		val field: String,

		@field:Schema(description = "실패 사유", example = "이메일 형식이 올바르지 않습니다.")
		val message: String,
	)

	companion object {

		/** [ErrorCode] 하나로 상태·본문이 모두 정해지므로 [ResponseEntity] 까지 완성해 돌려준다. */
		fun of(
			errorCode: ErrorCode,
			fieldErrors: List<FieldError>? = null,
		): ResponseEntity<ErrorResponse> =
			ResponseEntity.status(errorCode.status).body(
				ErrorResponse(
					code = errorCode.code,
					message = errorCode.message,
					fieldErrors = fieldErrors,
				)
			)
	}
}
