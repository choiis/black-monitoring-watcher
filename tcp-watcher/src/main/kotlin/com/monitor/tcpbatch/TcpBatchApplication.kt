package com.monitor.tcpbatch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.monitor.tcpbatch", "com.monitor.api"])
@EnableScheduling
@EnableFeignClients
class TcpBatchApplication

fun main(args: Array<String>) {
    runApplication<TcpBatchApplication>(*args)
}