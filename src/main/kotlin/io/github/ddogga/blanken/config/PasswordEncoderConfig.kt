package io.github.ddogga.blanken.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 비밀번호 해싱용 인코더.
 *
 * Spring Security 전체가 아니라 `spring-security-crypto` 만 의존하므로
 * 인증·인가 필터체인은 활성화되지 않는다. 인증을 붙일 때 이 빈을 그대로 재사용한다.
 */
@Configuration
class PasswordEncoderConfig {

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
