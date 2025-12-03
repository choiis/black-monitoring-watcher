package com.monitor.tcpbatch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.monitor.tcpbatch", "com.monitor.api"])
@EnableScheduling
class TcpBatchApplication

fun main(args: Array<String>) {
    runApplication<TcpBatchApplication>(*args)
}