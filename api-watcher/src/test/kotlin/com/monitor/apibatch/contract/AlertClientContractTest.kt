package com.monitor.apibatch.contract

import com.monitor.api.client.AlertClient
import com.monitor.api.dto.AlertRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties
import org.springframework.cloud.contract.stubrunner.StubFinder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.util.UUID

@SpringJUnitConfig
@AutoConfigureStubRunner(
    ids = ["com.monitor:api-server:+:stubs"],
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class AlertClientContractTest {

    @Autowired
    private lateinit var stubFinder: StubFinder

    private lateinit var alertClient: AlertClient

    @BeforeEach
    fun setup() {
        val stubUrl = stubFinder.findStubUrl("com.monitor", "api-server")
        alertClient = AlertClient(WebClient.builder(), stubUrl.toString())
    }

    @Test
    fun `should successfully send alert to api-server`() {
        val request = AlertRequest(
            serviceUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            scenarioUuid = UUID.fromString("660e8400-e29b-41d4-a716-446655440001"),
            serviceName = "test-service"
        )

        StepVerifier.create(alertClient.sendAlert(request))
            .verifyComplete()
    }

    @Test
    fun `should handle alert with random UUIDs`() {
        val request = AlertRequest(
            serviceUuid = UUID.randomUUID(),
            scenarioUuid = UUID.randomUUID(),
            serviceName = "api-watcher-test-service"
        )

        StepVerifier.create(alertClient.sendAlert(request))
            .verifyComplete()
    }
}
