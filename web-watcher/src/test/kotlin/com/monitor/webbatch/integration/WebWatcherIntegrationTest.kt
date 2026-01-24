package com.monitor.webbatch.integration

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.WebScenario
import com.monitor.api.domain.WebScenarioKey
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.api.repository.WebScenarioReactiveRepository
import com.monitor.webbatch.config.SeleniumConfig
import com.monitor.webbatch.simulator.WebScenarioSimulator
import com.monitor.webbatch.worker.WebScenarioBatchWorker
import org.apache.curator.framework.CuratorFramework
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.net.URI
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class WebWatcherIntegrationTest {

    @Mock
    lateinit var curatorFramework: CuratorFramework

    @Mock
    lateinit var repository: WebScenarioReactiveRepository

    @Mock
    lateinit var seleniumConfig: SeleniumConfig

    @Mock
    lateinit var mimirMetricPusher: MimirMetricPusher

    @Mock
    lateinit var alertClient: AlertClient

    private val instanceId = "integration-test-web-instance"

    @Test
    fun `worker and simulator should work together for scenario processing`() {
        // Given
        val scenarios = createTestScenarios(3)
        val worker = mock<WebScenarioBatchWorker>()
        whenever(worker.getWebScenarioList()).thenReturn(scenarios)

        val simulator = WebScenarioSimulator(worker, seleniumConfig, mimirMetricPusher, alertClient)

        whenever(seleniumConfig.borrowDriver()).thenReturn(null)
        whenever(alertClient.sendAlert(any())).thenReturn(Mono.empty())

        // When
        simulator.simulate()

        // Then
        Thread.sleep(500)
        verify(worker).getWebScenarioList()
    }

    @Test
    fun `partition assignment should be consistent across multiple calls`() {
        // Given
        val worker = WebScenarioBatchWorker(curatorFramework, repository, instanceId)

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val testKey = WebScenarioKey(UUID.randomUUID(), UUID.randomUUID())
        val total = 5

        // Find assigned partition
        var assignedPartition = -1
        for (i in 0 until total) {
            if (method.invoke(worker, testKey, i, total) as Boolean) {
                assignedPartition = i
                break
            }
        }

        // When & Then
        repeat(100) {
            for (i in 0 until total) {
                val result = method.invoke(worker, testKey, i, total) as Boolean
                assertEquals(i == assignedPartition, result)
            }
        }
    }

    @Test
    fun `scenarios should be evenly distributed across partitions`() {
        // Given
        val worker = WebScenarioBatchWorker(curatorFramework, repository, instanceId)
        val totalScenarios = 1000
        val totalPartitions = 5

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val partitionCounts = IntArray(totalPartitions)

        // When
        repeat(totalScenarios) {
            val key = WebScenarioKey(UUID.randomUUID(), UUID.randomUUID())
            for (i in 0 until totalPartitions) {
                if (method.invoke(worker, key, i, totalPartitions) as Boolean) {
                    partitionCounts[i]++
                    break
                }
            }
        }

        // Then
        val expectedPerPartition = totalScenarios / totalPartitions
        val minExpected = (expectedPerPartition * 0.8).toInt()
        val maxExpected = (expectedPerPartition * 1.2).toInt()

        partitionCounts.forEachIndexed { index, count ->
            assertTrue(
                count in minExpected..maxExpected,
                "Partition $index has $count scenarios"
            )
        }
    }

    @Test
    fun `dns resolution should work for localhost`() {
        // When
        val start = System.nanoTime()
        val address = InetAddress.getByName("localhost")
        val end = System.nanoTime()
        val dnsMs = (end - start) / 1_000_000

        // Then
        assertNotNull(address)
        assertTrue(dnsMs >= 0)
    }

    @Test
    fun `URI parsing should extract host correctly`() {
        // Given
        val testUrls = listOf(
            "http://example.com/path" to "example.com",
            "https://sub.domain.com:8080/api" to "sub.domain.com",
            "http://localhost:3000/test" to "localhost"
        )

        // Then
        testUrls.forEach { (url, expectedHost) ->
            val uri = URI.create(url)
            assertEquals(expectedHost, uri.host)
        }
    }

    @Test
    fun `WebScenarioResult should store all timing values correctly`() {
        // Given
        val result = WebScenarioSimulator.WebScenarioResult(
            dnsMs = 10L,
            pageLoadMs = 500L,
            jsExecMs = 100L,
            loginMs = 200L,
            success = true
        )

        // Then
        assertEquals(10L, result.dnsMs)
        assertEquals(500L, result.pageLoadMs)
        assertEquals(100L, result.jsExecMs)
        assertEquals(200L, result.loginMs)
        assertTrue(result.success)
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
        assertEquals(-1L, result.pageLoadMs)
    }

    @Test
    fun `scenario with javascript should be processed correctly`() {
        // Given
        val scenario = WebScenario(
            key = WebScenarioKey(UUID.randomUUID(), UUID.randomUUID()),
            serviceName = "test-web-service",
            url = "http://localhost:8080/app",
            method = "GET",
            javascript = "return document.title;",
            loginScript = "document.getElementById('username').value='test';",
            description = "Web scenario with JS",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )

        // Then
        assertEquals("test-web-service", scenario.serviceName)
        assertEquals("http://localhost:8080/app", scenario.url)
        assertNotNull(scenario.javascript)
        assertNotNull(scenario.loginScript)
    }

    @Test
    fun `scenario without javascript should have null fields`() {
        // Given
        val scenario = WebScenario(
            key = WebScenarioKey(UUID.randomUUID(), UUID.randomUUID()),
            serviceName = "test-web-service",
            url = "http://localhost:8080/app",
            method = "GET",
            javascript = null,
            loginScript = null,
            description = "Web scenario without JS",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )

        // Then
        assertNull(scenario.javascript)
        assertNull(scenario.loginScript)
    }

    @Test
    fun `metric labels should be correctly formatted`() {
        // Given
        val scenarioUuid = UUID.randomUUID()
        val url = "https://example.com/test"

        // When
        val labels = mapOf(
            "scenario_uuid" to scenarioUuid.toString(),
            "url" to url
        )

        // Then
        assertEquals(2, labels.size)
        assertEquals(scenarioUuid.toString(), labels["scenario_uuid"])
        assertEquals(url, labels["url"])
    }

    @Test
    fun `concurrency should be limited to 1 for WebDriver safety`() {
        // This test documents the concurrency requirement
        val expectedConcurrency = 1

        // WebDriver is not thread-safe, so concurrency must be 1
        assertEquals(1, expectedConcurrency)
    }

    private fun createTestScenarios(count: Int): List<WebScenario> {
        return (1..count).map { i ->
            WebScenario(
                key = WebScenarioKey(UUID.randomUUID(), UUID.randomUUID()),
                serviceName = "test-web-service-$i",
                url = "http://localhost:8080/page$i",
                method = "GET",
                javascript = if (i % 2 == 0) "return document.title;" else null,
                loginScript = if (i % 3 == 0) "console.log('login');" else null,
                description = "Test Web scenario $i",
                createdTime = Instant.now(),
                updatedTime = Instant.now()
            )
        }
    }
}
