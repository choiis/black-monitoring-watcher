package com.monitor.apibatch.integration

import com.monitor.api.domain.ApiScenario
import com.monitor.api.domain.ApiScenarioKey
import com.monitor.api.repository.ApiScenarioReactiveRepository
import com.monitor.apibatch.worker.ApiScenarioBatchWorker
import org.apache.curator.framework.CuratorFramework
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpMethod
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ApiWatcherIntegrationTest {

    @Mock
    lateinit var curatorFramework: CuratorFramework

    @Mock
    lateinit var repository: ApiScenarioReactiveRepository

    private val instanceId = "integration-test-instance"

    private lateinit var worker: ApiScenarioBatchWorker

    @BeforeEach
    fun setUp() {
        worker = ApiScenarioBatchWorker(curatorFramework, repository, instanceId)
    }

    @Test
    fun `partition assignment should be consistent across multiple calls`() {
        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val testKey = ApiScenarioKey(UUID.randomUUID(), UUID.randomUUID())
        val total = 5

        // Find assigned partition
        var assignedPartition = -1
        for (i in 0 until total) {
            if (method.invoke(worker, testKey, i, total) as Boolean) {
                assignedPartition = i
                break
            }
        }

        // Verify consistency
        repeat(100) {
            for (i in 0 until total) {
                val result = method.invoke(worker, testKey, i, total) as Boolean
                assertEquals(i == assignedPartition, result)
            }
        }
    }

    @Test
    fun `scenarios should be evenly distributed across partitions`() {
        val totalScenarios = 1000
        val totalPartitions = 5

        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val partitionCounts = IntArray(totalPartitions)

        // Distribute scenarios
        repeat(totalScenarios) {
            val key = ApiScenarioKey(UUID.randomUUID(), UUID.randomUUID())
            for (i in 0 until totalPartitions) {
                if (method.invoke(worker, key, i, totalPartitions) as Boolean) {
                    partitionCounts[i]++
                    break
                }
            }
        }

        // Verify even distribution (within 20% variance)
        val expectedPerPartition = totalScenarios / totalPartitions
        val minExpected = (expectedPerPartition * 0.8).toInt()
        val maxExpected = (expectedPerPartition * 1.2).toInt()

        partitionCounts.forEachIndexed { index, count ->
            assertTrue(
                count in minExpected..maxExpected,
                "Partition $index has $count scenarios, expected between $minExpected and $maxExpected"
            )
        }
    }

    @Test
    fun `http methods should be correctly mapped`() {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

        methods.forEach { methodStr ->
            val method = HttpMethod.valueOf(methodStr)
            assertEquals(methodStr, method.name())
        }
    }

    @Test
    fun `scenario with all fields should be processed correctly`() {
        val scenario = ApiScenario(
            key = ApiScenarioKey(UUID.randomUUID(), UUID.randomUUID()),
            serviceName = "test-service",
            url = "http://localhost:8080/api/test",
            method = "POST",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer token123"
            ),
            requestBody = """{"key": "value"}""",
            description = "Test scenario with all fields",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )

        assertEquals("test-service", scenario.serviceName)
        assertEquals("http://localhost:8080/api/test", scenario.url)
        assertEquals("POST", scenario.method)
        assertEquals(2, scenario.headers?.size)
        assertEquals("""{"key": "value"}""", scenario.requestBody)
    }

    @Test
    fun `every scenario should be assigned to exactly one partition`() {
        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val totalPartitions = 5

        repeat(100) {
            val key = ApiScenarioKey(UUID.randomUUID(), UUID.randomUUID())
            var matchCount = 0

            for (i in 0 until totalPartitions) {
                if (method.invoke(worker, key, i, totalPartitions) as Boolean) {
                    matchCount++
                }
            }

            assertEquals(1, matchCount, "Each key should match exactly one partition")
        }
    }

    @Test
    fun `worker should return empty list initially`() {
        val scenarios = worker.getApiScenarioList()
        assertTrue(scenarios.isEmpty())
    }
}
