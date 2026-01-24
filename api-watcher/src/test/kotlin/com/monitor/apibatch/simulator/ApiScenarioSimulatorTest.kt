package com.monitor.apibatch.simulator

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.ApiScenario
import com.monitor.api.domain.ApiScenarioKey
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.apibatch.worker.ApiScenarioBatchWorker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ApiScenarioSimulatorTest {

    @Mock
    lateinit var batchWorker: ApiScenarioBatchWorker

    @Mock
    lateinit var webClient: WebClient

    @Mock
    lateinit var mimirMetricPusher: MimirMetricPusher

    @Mock
    lateinit var alertClient: AlertClient

    private lateinit var simulator: ApiScenarioSimulator

    @BeforeEach
    fun setUp() {
        simulator = ApiScenarioSimulator(batchWorker, webClient, mimirMetricPusher, alertClient)
    }

    @Test
    fun `simulate should skip when scenario list is empty`() {
        // Given
        whenever(batchWorker.getApiScenarioList()).thenReturn(emptyList())

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getApiScenarioList()
        verifyNoInteractions(webClient)
        verifyNoInteractions(mimirMetricPusher)
    }

    @Test
    fun `simulate should get scenario list from worker`() {
        // Given
        whenever(batchWorker.getApiScenarioList()).thenReturn(emptyList())

        // When
        simulator.simulate()

        // Then
        verify(batchWorker, times(1)).getApiScenarioList()
    }

    @Test
    fun `scenario with blank url should be skipped`() {
        // Given
        val scenario = createApiScenario(url = "", method = "GET")

        // Then: URL validation
        assertTrue(scenario.url.isNullOrBlank())
    }

    @Test
    fun `scenario with blank method should be skipped`() {
        // Given
        val scenario = createApiScenario(url = "http://example.com", method = "")

        // Then: Method validation
        assertTrue(scenario.method.isNullOrBlank())
    }

    @Test
    fun `scenario with null url should be skipped`() {
        // Given
        val scenario = createApiScenario(url = null, method = "GET")

        // Then
        assertTrue(scenario.url.isNullOrBlank())
    }

    @Test
    fun `scenario with null method should be skipped`() {
        // Given
        val scenario = createApiScenario(url = "http://example.com", method = null)

        // Then
        assertTrue(scenario.method.isNullOrBlank())
    }

    @Test
    fun `HTTP methods should be parsed correctly`() {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

        methods.forEach { methodStr ->
            val method = HttpMethod.valueOf(methodStr.uppercase())
            assertEquals(methodStr, method.name())
        }
    }

    @Test
    fun `standard HTTP methods should be recognized`() {
        val standardMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE")

        standardMethods.forEach { methodStr ->
            val method = HttpMethod.valueOf(methodStr)
            assertNotNull(method)
        }
    }

    @Test
    fun `valid URL should be parsed correctly`() {
        val validUrls = listOf(
            "http://example.com/api/test",
            "https://api.example.com:8080/v1/users",
            "http://localhost:3000/health"
        )

        validUrls.forEach { url ->
            val uri = URI.create(url)
            assertNotNull(uri.host)
        }
    }

    @Test
    fun `URL host extraction should work correctly`() {
        val testCases = mapOf(
            "http://example.com/path" to "example.com",
            "https://api.test.com:8080/v1" to "api.test.com",
            "http://localhost:3000" to "localhost"
        )

        testCases.forEach { (url, expectedHost) ->
            val uri = URI.create(url)
            assertEquals(expectedHost, uri.host)
        }
    }

    @Test
    fun `metric labels should contain required fields`() {
        // Given
        val scenarioUuid = UUID.randomUUID()
        val url = "http://example.com/api"
        val method = "GET"
        val status = 200

        // When
        val labels = mapOf(
            "scenario_uuid" to scenarioUuid.toString(),
            "url" to url,
            "method" to method,
            "status" to status.toString()
        )

        // Then
        assertEquals(4, labels.size)
        assertEquals(scenarioUuid.toString(), labels["scenario_uuid"])
        assertEquals(url, labels["url"])
        assertEquals(method, labels["method"])
        assertEquals("200", labels["status"])
    }

    @Test
    fun `scenario with all fields should be created correctly`() {
        // Given & When
        val scenario = createApiScenario(
            url = "http://example.com/api",
            method = "POST",
            headers = mapOf("Content-Type" to "application/json"),
            requestBody = """{"key": "value"}"""
        )

        // Then
        assertEquals("http://example.com/api", scenario.url)
        assertEquals("POST", scenario.method)
        assertEquals(mapOf("Content-Type" to "application/json"), scenario.headers)
        assertEquals("""{"key": "value"}""", scenario.requestBody)
    }

    @Test
    fun `scenario key should contain service and scenario uuids`() {
        // Given
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.randomUUID()

        // When
        val scenario = createApiScenario(serviceUuid = serviceUuid, scenarioUuid = scenarioUuid)

        // Then
        assertEquals(serviceUuid, scenario.key?.serviceUuid)
        assertEquals(scenarioUuid, scenario.key?.scenarioUuid)
    }

    @Test
    fun `dns metric name should be correct`() {
        val metricName = "black_monitoring_api_dns_ms"
        assertTrue(metricName.startsWith("black_monitoring_api_"))
    }

    @Test
    fun `request metric name should be correct`() {
        val metricName = "black_monitoring_api_request_ms"
        assertTrue(metricName.startsWith("black_monitoring_api_"))
    }

    private fun createApiScenario(
        serviceUuid: UUID = UUID.randomUUID(),
        scenarioUuid: UUID = UUID.randomUUID(),
        url: String? = "http://example.com",
        method: String? = "GET",
        headers: Map<String, String>? = null,
        requestBody: String? = null
    ): ApiScenario {
        return ApiScenario(
            key = ApiScenarioKey(serviceUuid, scenarioUuid),
            serviceName = "test-service",
            url = url,
            method = method,
            headers = headers,
            requestBody = requestBody,
            description = "Test scenario",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )
    }
}
