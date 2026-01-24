package com.monitor.api.controller

import com.monitor.api.dto.AlertRequest
import com.monitor.api.service.ScenarioFailAlertService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.*

@WebFluxTest(controllers = [AlertController::class])
class AlertControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockBean
    lateinit var scenarioFailAlertService: ScenarioFailAlertService

    @Test
    fun `POST alert should return 202 and delegate to service`() {
        // given
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.randomUUID()
        val serviceName = "order-service-scenario"

        val request = AlertRequest(
            serviceUuid = serviceUuid,
            scenarioUuid = scenarioUuid,
            serviceName = serviceName
        )

        whenever(scenarioFailAlertService.notifyScenarioFailed(eq(serviceUuid), eq(serviceName)))
            .thenReturn(Mono.empty())

        // when & then
        webTestClient.post()
            .uri("/api/v1/alert")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted

        verify(scenarioFailAlertService)
            .notifyScenarioFailed(eq(serviceUuid), eq(serviceName))
    }
}