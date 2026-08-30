package io.github.ddogga.blanken.exception

import org.springframework.http.HttpStatus

/**
 * 코드 체계는 `앞자리 알파벳(도메인) + 3자리 순번`
 * - `C` — Common. 특정 도메인에 속하지 않는 횡단 관심사(검증 실패, 무결성 위반, 서버 오류)
 * - `U` — User
 * - `A` — Auth
 *
 * 순번은 도메인별로 `001`부터 매기고, **`999`는 그 도메인의 서버 오류·기타용으로 예약**한다.
 *
 * `message`는 클라이언트에 그대로 나가는 값이므로 내부 사정(id, 테이블명, 스택)을 담지 않는다.
 * 진단에 필요한 맥락은 예외의 message에 담아 로그로만 남긴다.
 */
enum class ErrorCode(
	val status: HttpStatus,
	val code: String,
	val message: String,
) {

	// Common — 횡단 관심사
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
	DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "C002", "다른 데이터가 참조하고 있어 처리할 수 없습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다."),

	// User
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "유저를 찾을 수 없습니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),

	// Auth
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "A001", "현재 비밀번호가 일치하지 않습니다."),
	;
}
