package io.github.ddogga.blanken.exception


class UserNotFoundException(val userId: Long) :
	BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다. (id=$userId)")

class DuplicateEmailException(val email: String) :
	BusinessException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다. (email=$email)")

class InvalidPasswordException :
	BusinessException(ErrorCode.INVALID_PASSWORD)
