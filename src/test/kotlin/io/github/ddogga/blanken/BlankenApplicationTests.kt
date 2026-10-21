package io.github.ddogga.blanken

import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 로컬 PostgreSQL 이 아니라 Testcontainers DB 에 붙는다.
 * 빈 컨테이너에 Flyway 가 V1 부터 적용하므로 `ddl-auto: validate` 와 마이그레이션의 일치 여부도 함께 검증된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig::class)
class BlankenApplicationTests {

	@Test
	fun contextLoads() {
	}

}
