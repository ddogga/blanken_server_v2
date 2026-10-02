package io.github.ddogga.blanken.dto.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration


/**
 * application.yml의 app.jwt.* 값이 바인딩 됨.
 */
@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String,             // Base64 인코딩된 32바이트 이상 키
    val accessTokenExpiry : Duration,
    val refreshTokenExpiry: Duration,
)