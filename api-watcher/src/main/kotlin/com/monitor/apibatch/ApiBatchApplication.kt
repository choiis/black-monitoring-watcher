package com.monitor.apibatch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.monitor.apibatch", "com.monitor.api"])
@EnableScheduling
@EnableFeignClients
class ApiBatchApplication

fun main(args: Array<String>) {
    runApplication<ApiBatchApplication>(*args)
}