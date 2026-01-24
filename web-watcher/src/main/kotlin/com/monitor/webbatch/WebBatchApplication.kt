package com.monitor.webbatch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.monitor.webbatch", "com.monitor.api"])
@EnableScheduling
@EnableFeignClients
class WebBatchApplication

fun main(args: Array<String>) {
    runApplication<WebBatchApplication>(*args)
}
