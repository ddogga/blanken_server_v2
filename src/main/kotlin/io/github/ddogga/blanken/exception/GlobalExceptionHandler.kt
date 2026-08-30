package io.github.ddogga.blanken.exception

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 클라이언트에 나가는 메시지는 언제나 [ErrorCode]의 message.
 * 예외가 들고 온 원본 메시지는 예외 원인(id·이메일 등)을 담고 있어 로그로만 남긴다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	/** 모든 도메인 예외의 단일 진입점. 새 예외는 [BusinessException] 을 상속하기만 하면 여기로 들어온다. */
	@ExceptionHandler(BusinessException::class)
	fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
		log.warn("도메인 예외: code={}, detail={}", e.errorCode.code, e.message)
		return ErrorResponse.of(e.errorCode)
	}

	/** `@Valid` 검증 실패. 어떤 필드가 왜 틀렸는지 함께 돌려준다. */
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
		val fieldErrors = e.bindingResult.fieldErrors.map {
			ErrorResponse.FieldError(
				field = it.field,
				message = it.defaultMessage ?: "올바르지 않은 값입니다.",
			)
		}
		return ErrorResponse.of(ErrorCode.VALIDATION_FAILED, fieldErrors)
	}

	/**
	 * DB 제약 위반 중 도메인 예외로 변환하지 않은 것들.
	 * 대표적으로 다른 데이터가 참조 중인 유저를 삭제하려 할 때의 FK 위반이 여기로 온다.
	 */
	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
		log.warn("데이터 무결성 제약 위반", e)
		return ErrorResponse.of(ErrorCode.DATA_INTEGRITY_VIOLATION)
	}
}
