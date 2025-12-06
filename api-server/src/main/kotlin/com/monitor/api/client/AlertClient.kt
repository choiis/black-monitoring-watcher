package com.monitor.api.client

import com.monitor.api.dto.AlertRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class AlertClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${api-server.base-url}") private val baseUrl: String
) {

    private val log = LoggerFactory.getLogger(AlertClient::class.java)

    private val client: WebClient = webClientBuilder
        .baseUrl(baseUrl)
        .build()

    fun sendAlert(request: AlertRequest): Mono<Void> {
        return client.post()
            .uri("/api/v1/alert")
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .doOnNext {
                log.info(
                    "Alert sent successfully to monitoring-api: serviceUuid={}, scenarioUuid={}, serviceName={}",
                    request.serviceUuid,
                    request.scenarioUuid,
                    request.serviceName
                )
            }
            .doOnError { ex ->
                log.warn(
                    "Failed to send alert to monitoring-api: serviceUuid={}, scenarioUuid={}, serviceName={}, reason={}",
                    request.serviceUuid,
                    request.scenarioUuid,
                    request.serviceName,
                    ex.toString()
                )
            }
            .onErrorResume { Mono.empty() }
            .then()
    }
}