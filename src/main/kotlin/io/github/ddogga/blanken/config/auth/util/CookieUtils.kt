package io.github.ddogga.blanken.config.auth.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.Duration
import java.util.Base64

object CookieUtils {


    fun getCookie(request: HttpServletRequest, name: String): Cookie? =
        request.cookies?.firstOrNull { it.name == name}

    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Duration) {

        val cookie = ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(true)
            .sameSite("Lax")
            .maxAge(maxAge)
//            .secure(true) //TODO: 운영에서 활성화
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }


    /** 같은 이름, 경로로 maxAge = 0 쿠키를 보내 브라우저가 삭제하게 한다*/
    fun deleteCookie(response : HttpServletResponse, name: String) {
        addCookie(response, name, "", Duration.ZERO)
    }


    /** 객체 -> Java 직렬화 -> URL-safe Base64 문자열 (쓸 수 없는 문자 회피)*/
    fun serialize(obj: Serializable): String {
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(obj) }
            bos.toByteArray()
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * 문자열 -> 객체 파싱. 형식이 깨졌거나 타입이 다르면 null
     * TODO: (보안) 쿠키는 클라이언트가 조작 가능하므로 신뢰할 수 없는 값을 역직렬화 하는 건 위험함 추후 Redis + 랜덤 키 방식으로 교체할 예정
     */

    /**
     * reified -> as? T 타입 비교시 제네릭 타입을 실행 시점에서 쓰기 위해 inline 코드 선언 및 reified 키워드 사용.
     */
    inline fun <reified T> deserialize(value: String): T? =
        runCatching {
            val bytes = Base64.getUrlDecoder().decode(value)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as? T
        }.getOrNull()



}