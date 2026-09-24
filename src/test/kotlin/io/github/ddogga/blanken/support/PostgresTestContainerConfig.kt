package io.github.ddogga.blanken.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer


@TestConfiguration(proxyBeanMethods = false)
class PostgresTestContainerConfig {

	@Bean
	@ServiceConnection
	fun postgresContainer(): PostgreSQLContainer = CONTAINER

	companion object {
		/** 운영 DB 와 메이저 버전을 맞춘다. 로컬 PostgreSQL 버전이 바뀌면 여기도 같이 올릴 것. */
		private const val IMAGE = "postgres:17-alpine"

		/**
		 * pgAdmin 접속용 고정 호스트 포트. 로컬 PostgreSQL 의 5432 와 겹치지 않게 멀리 띄워 뒀다.
		 *
		 * 고정하지 않으면 매 실행마다 임의 포트라 `docker ps` 로 찾아야 한다.
		 * 대신 이 포트가 점유돼 있으면 컨테이너 기동이 실패하고, 테스트를 병렬로 돌릴 수 없다.
		 * 병렬 실행이 필요해지면 아래 setPortBindings 를 지울 것.
		 */
		const val HOST_PORT = 55432

		private val CONTAINER: PostgreSQLContainer = PostgreSQLContainer(IMAGE).apply {
			withDatabaseName(DATABASE_NAME)
			withUsername(USERNAME)
			withPassword(PASSWORD)
			// withExposedPorts 와 달리 호스트 쪽 포트를 지정한다. 임의 매핑을 덮어쓴다.
			setPortBindings(listOf("$HOST_PORT:5432"))
			start()
		}

		const val DATABASE_NAME = "blanken_test"
		const val USERNAME = "test"
		const val PASSWORD = "test"
	}
}
