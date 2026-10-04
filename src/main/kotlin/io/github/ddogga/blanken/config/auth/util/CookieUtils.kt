package io.github.ddogga.blanken.config.auth.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import java.time.Duration

object CookieUtils {


    fun getCookie(request: HttpServletRequest, name: String): Cookie? =
        request.cookies?.firstOrNull { it.name == name }

    fun addCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        maxAge: Duration,
        path: String = "/",
        ) {
        val cookie = ResponseCookie.from(name, value)
            .path(path)
            .httpOnly(true)
            .sameSite("Lax")
            .maxAge(maxAge)
//            .secure(true) //TODO: 운영에서 활성화
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }


    /** 같은 이름, 경로로 maxAge = 0 쿠키를 보내 브라우저가 삭제하게 한다*/
    fun deleteCookie(response : HttpServletResponse, name: String, path: String = "/") {
        addCookie(response, name, "", Duration.ZERO, path)
    }




}