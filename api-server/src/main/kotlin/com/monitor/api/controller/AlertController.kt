package com.monitor.api.controller

import com.monitor.api.dto.AlertRequest
import com.monitor.api.service.ScenarioFailAlertService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/api/v1/alert")
class AlertController(
    private val scenarioFailAlertService: ScenarioFailAlertService
) {

    private val log = LoggerFactory.getLogger(AlertController::class.java)

    @PostMapping
    fun createAlert(@RequestBody request: AlertRequest): Mono<ResponseEntity<Void>> {
        log.warn(
            "Received alert: serviceUuid={}, scenarioUuid={}, serviceName={}",
            request.serviceUuid,
            request.scenarioUuid,
            request.serviceName
        )

        return scenarioFailAlertService
            .notifyScenarioFailed(request.serviceUuid, request.serviceName)
            .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build())
    }
}