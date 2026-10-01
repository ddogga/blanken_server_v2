package io.github.ddogga.blanken.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain


@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {
    @Bean
    fun testFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            authorizeHttpRequests { authorize(anyRequest, permitAll) }
        }
        return http.build()
    }
}