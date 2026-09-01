package io.github.ddogga.blanken.exception

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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
	 * 요청 본문 자체를 읽지 못한 경우 — 깨진 JSON, 필수 필드 누락, 잘못된 enum 값 등.
	 *
	 * Bean Validation 은 역직렬화가 **성공한 뒤에** 돈다. Kotlin 논-널 필드가 비어 있으면
	 * `@Valid` 가 돌기도 전에 Jackson 이 먼저 실패하므로, 이 핸들러가 없으면
	 * 스프링 기본 에러 바디(`timestamp`/`status`/`error`/`path`)가 나가 응답 형식이 두 개가 된다.
	 *
	 * 어느 필드가 문제인지는 파서 예외 메시지에 내부 타입 정보가 섞여 있어 그대로 내보내지 않고 로그로만 남긴다.
	 */
	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
		log.warn("요청 본문을 읽을 수 없음", e)
		return ErrorResponse.of(ErrorCode.VALIDATION_FAILED)
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
