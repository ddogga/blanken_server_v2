package io.github.ddogga.blanken.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * [io.github.ddogga.blanken.domain.BaseTimeEntity] 의 createdAt 자동 설정을 활성화한다.
 */
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig
