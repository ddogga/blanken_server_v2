package io.github.ddogga.blanken

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan // @ConfigurationProperties 스캔
@SpringBootApplication
class BlankenApplication

fun main(args: Array<String>) {
	runApplication<BlankenApplication>(*args)
}
