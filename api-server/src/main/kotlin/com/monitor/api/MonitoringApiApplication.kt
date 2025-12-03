package com.monitor.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MonitoringApiApplication

fun main(args: Array<String>) {
    runApplication<MonitoringApiApplication>(*args)
}