package io.github.ddogga.blanken.config.auth

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType


/** 인증, 인가 실패 응답 코드. 프론트에서 code 값으로 대응을 분기함.*/
enum class AuthErrorCode(
    val status: HttpStatus,
    val message: String,
) {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),        // 토큰 없음 -> 로그인
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),    // 만료 -> /auth/refresh 재발급 시도
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),  // 위조, 형식 오류 -> 로그인
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."), // 재발급 실패 -> 로그인
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");        // 인증은 됐지만 권한 부족


    companion object {
        /** 필터가 검증 실패 원인을 EntryPoint에 전달할 때 쓰는 request attribute 키 */
        const val REQUEST_ATTRIBUTE = "authErrorCode"
    }
}


/**
 * 에러 응답을 JSON으로 작성.
 * 값이 모두 고정 상수라 ObjectMapper 없이 문자열로 만든다 (Jackson 버전과 무관하게 동작).
 *
 * 확장 함수 사용 : HttpServletResponse의 확장함수로서 status나 contentType의 변수에 접근
 */
fun HttpServletResponse.writeAuthError(code: AuthErrorCode) {
    status = code.status.value()
    contentType = MediaType.APPLICATION_JSON_VALUE
    characterEncoding = Charsets.UTF_8.name()
    writer.write("""{"code":"${code.name}","message":"${code.message}"}""")
}