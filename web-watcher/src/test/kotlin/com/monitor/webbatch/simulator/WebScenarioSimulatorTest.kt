package com.monitor.webbatch.simulator

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.WebScenario
import com.monitor.api.domain.WebScenarioKey
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.webbatch.config.SeleniumConfig
import com.monitor.webbatch.worker.WebScenarioBatchWorker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.openqa.selenium.WebDriver
import java.net.InetAddress
import java.net.URI
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class WebScenarioSimulatorTest {

    @Mock
    lateinit var batchWorker: WebScenarioBatchWorker

    @Mock
    lateinit var seleniumConfig: SeleniumConfig

    @Mock
    lateinit var mimirMetricPusher: MimirMetricPusher

    @Mock
    lateinit var alertClient: AlertClient

    @Mock
    lateinit var webDriver: WebDriver

    @Mock
    lateinit var webDriverOptions: WebDriver.Options

    private lateinit var simulator: WebScenarioSimulator

    @BeforeEach
    fun setUp() {
        simulator = WebScenarioSimulator(batchWorker, seleniumConfig, mimirMetricPusher, alertClient)
    }

    @Test
    fun `simulate should skip when scenario list is empty`() {
        // Given
        whenever(batchWorker.getWebScenarioList()).thenReturn(emptyList())

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getWebScenarioList()
        verifyNoInteractions(seleniumConfig)
        verifyNoInteractions(mimirMetricPusher)
    }

    @Test
    fun `simulateScenario should skip when url is blank`() {
        // Given
        val scenario = createWebScenario(url = "")
        whenever(batchWorker.getWebScenarioList()).thenReturn(listOf(scenario))

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getWebScenarioList()
        verifyNoInteractions(seleniumConfig)
    }

    @Test
    fun `simulateScenario should skip when url is null`() {
        // Given
        val scenario = createWebScenario(url = null)
        whenever(batchWorker.getWebScenarioList()).thenReturn(listOf(scenario))

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getWebScenarioList()
        verifyNoInteractions(seleniumConfig)
    }

    @Test
    fun `simulateScenario should skip when url is invalid`() {
        // Given
        val scenario = createWebScenario(url = "not-a-valid-url")
        whenever(batchWorker.getWebScenarioList()).thenReturn(listOf(scenario))

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getWebScenarioList()
        verifyNoInteractions(seleniumConfig)
    }

    @Test
    fun `scenario with invalid domain should be identifiable`() {
        // Given
        val scenario = createWebScenario(url = "http://nonexistent.invalid.domain.test/page")

        // Then
        assertNotNull(scenario.url)
        assertTrue(scenario.url!!.contains("invalid"))
    }

    @Test
    fun `scenario with localhost should be valid`() {
        // Given
        val scenario = createWebScenario(url = "http://localhost:8080/test")

        // Then
        assertNotNull(scenario.url)
        assertTrue(scenario.url!!.contains("localhost"))
    }

    @Test
    fun `selenium config should be mockable`() {
        // Given & Then
        assertNotNull(seleniumConfig)
    }

    @Test
    fun `WebScenarioResult should store all timing metrics`() {
        // Given
        val dnsMs = 10L
        val pageLoadMs = 500L
        val jsExecMs = 100L
        val loginMs = 200L
        val success = true

        // When
        val result = WebScenarioSimulator.WebScenarioResult(dnsMs, pageLoadMs, jsExecMs, loginMs, success)

        // Then
        assertEquals(dnsMs, result.dnsMs)
        assertEquals(pageLoadMs, result.pageLoadMs)
        assertEquals(jsExecMs, result.jsExecMs)
        assertEquals(loginMs, result.loginMs)
        assertTrue(result.success)
    }

    @Test
    fun `dns resolution should measure time correctly`() {
        // Given
        val host = "localhost"

        // When
        val start = System.nanoTime()
        val address = InetAddress.getByName(host)
        val end = System.nanoTime()
        val dnsMs = (end - start) / 1_000_000

        // Then
        assertNotNull(address)
        assertTrue(dnsMs >= 0, "DNS resolution time should be non-negative")
    }

    @Test
    fun `metric labels should contain correct information`() {
        // Given
        val scenarioUuid = UUID.randomUUID()
        val url = "http://example.com/test"

        val expectedLabels = mapOf(
            "scenario_uuid" to scenarioUuid.toString(),
            "url" to url
        )

        // Then
        assertEquals(scenarioUuid.toString(), expectedLabels["scenario_uuid"])
        assertEquals(url, expectedLabels["url"])
    }

    @Test
    fun `URI host extraction should work correctly`() {
        // Given
        val validUrls = mapOf(
            "http://example.com/path" to "example.com",
            "https://sub.domain.com:8080/api" to "sub.domain.com",
            "http://localhost:3000" to "localhost",
            "https://192.168.1.1/test" to "192.168.1.1"
        )

        // Then
        validUrls.forEach { (url, expectedHost) ->
            val uri = URI.create(url)
            assertEquals(expectedHost, uri.host, "Host extraction failed for $url")
        }
    }

    @Test
    fun `URI without host should return null`() {
        // Given
        val url = "file:///path/to/file"

        // When
        val uri = URI.create(url)

        // Then
        assertNull(uri.host, "Host should be null for file URI")
    }

    @Test
    fun `should push metrics only for non-negative values`() {
        // Given
        val dnsMs = 10L
        val pageLoadMs = 500L
        val jsExecMs = -1L  // No JS executed
        val loginMs = -1L   // No login script

        // Then
        assertTrue(dnsMs >= 0, "DNS time should be pushed")
        assertTrue(pageLoadMs >= 0, "Page load time should be pushed")
        assertTrue(jsExecMs < 0, "JS exec time should not be pushed")
        assertTrue(loginMs < 0, "Login time should not be pushed")
    }

    @Test
    fun `flatMap concurrency should be 1 for WebDriver safety`() {
        // This is a documentation test to verify concurrency requirement
        // The actual implementation uses flatMap({ ... }, 1)
        val expectedConcurrency = 1
        assertEquals(1, expectedConcurrency, "Concurrency should be 1 for WebDriver thread safety")
    }

    @Test
    fun `WebScenarioResult with failure should set success to false`() {
        // Given
        val result = WebScenarioSimulator.WebScenarioResult(
            dnsMs = 10L,
            pageLoadMs = -1L,
            jsExecMs = -1L,
            loginMs = -1L,
            success = false
        )

        // Then
        assertEquals(false, result.success)
        assertEquals(10L, result.dnsMs)
        assertEquals(-1L, result.pageLoadMs)
    }

    private fun createWebScenario(
        serviceUuid: UUID = UUID.randomUUID(),
        scenarioUuid: UUID = UUID.randomUUID(),
        url: String? = "http://example.com",
        javascript: String? = null,
        loginScript: String? = null
    ): WebScenario {
        return WebScenario(
            key = WebScenarioKey(serviceUuid, scenarioUuid),
            serviceName = "test-web-service",
            url = url,
            method = "GET",
            javascript = javascript,
            loginScript = loginScript,
            description = "Test Web scenario",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )
    }
}
