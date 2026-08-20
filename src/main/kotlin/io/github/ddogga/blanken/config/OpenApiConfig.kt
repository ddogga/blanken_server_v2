package io.github.ddogga.blanken.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

	@Bean
	fun openAPI(): OpenAPI = OpenAPI()
		.info(
			Info()
				.title("Blanken API")
				.description("Blanken 서버 API 문서")
				.version("v0.0.1")
		)
}
