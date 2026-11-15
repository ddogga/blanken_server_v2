package io.github.ddogga.blanken.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {


    @Bean
    fun openAPI(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)   // HTTP 인증 방식
                        .scheme("bearer")                 // Authorization: Bearer <토큰>
                        .bearerFormat("JWT"),             // 문서 표시용 (동작에는 영향 없음)
                ),
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))  // 모든 API에 적용
            .info(
                Info()
                    .title("Blanken API")
                    .description("Blanken 서버 API 문서")
                    .version("v0.0.1")
            )
    }

}
