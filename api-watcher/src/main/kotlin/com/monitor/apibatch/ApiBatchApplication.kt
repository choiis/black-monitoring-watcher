package com.monitor.apibatch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.monitor.apibatch", "com.monitor.api"])
@EnableScheduling
class ApiBatchApplication

fun main(args: Array<String>) {
    runApplication<ApiBatchApplication>(*args)
}