package io.github.ddogga.blanken.exception


abstract class BusinessException(
	val errorCode: ErrorCode,
	detail: String? = null,
) : RuntimeException(detail ?: errorCode.message)
