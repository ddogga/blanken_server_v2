package io.github.ddogga.blanken

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BlankenApplication

fun main(args: Array<String>) {
	runApplication<BlankenApplication>(*args)
}
