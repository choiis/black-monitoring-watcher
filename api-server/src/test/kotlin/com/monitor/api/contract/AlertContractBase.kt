package com.monitor.api.contract

import com.monitor.api.controller.AlertController
import com.monitor.api.service.ScenarioFailAlertService
import io.restassured.module.webtestclient.RestAssuredWebTestClient
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

abstract class AlertContractBase {

    private val scenarioFailAlertService: ScenarioFailAlertService = mock()

    @BeforeEach
    fun setup() {
        whenever(scenarioFailAlertService.notifyScenarioFailed(any(), any()))
            .thenReturn(Mono.empty())

        val alertController = AlertController(scenarioFailAlertService)
        RestAssuredWebTestClient.standaloneSetup(alertController)
    }
}
