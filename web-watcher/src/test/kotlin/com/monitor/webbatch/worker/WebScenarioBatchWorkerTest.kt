package com.monitor.webbatch.worker

import com.monitor.api.domain.WebScenarioKey
import com.monitor.api.repository.WebScenarioReactiveRepository
import org.apache.curator.framework.CuratorFramework
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class WebScenarioBatchWorkerTest {

    @Mock
    lateinit var curatorFramework: CuratorFramework

    @Mock
    lateinit var repository: WebScenarioReactiveRepository

    private val instanceId = "test-web-instance-001"

    private lateinit var worker: WebScenarioBatchWorker

    @BeforeEach
    fun setUp() {
        worker = WebScenarioBatchWorker(curatorFramework, repository, instanceId)
    }

    @Test
    fun `isMyPartition should return true when hash mod equals index`() {
        // Given
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val key = WebScenarioKey(serviceUuid, scenarioUuid)

        val hash = scenarioUuid.hashCode()
        val total = 3
        val expectedIndex = Math.floorMod(hash, total)

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // When & Then
        val result = method.invoke(worker, key, expectedIndex, total) as Boolean
        assertTrue(result, "Should return true when hash mod equals index")
    }

    @Test
    fun `isMyPartition should return false when hash mod does not equal index`() {
        // Given
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val key = WebScenarioKey(serviceUuid, scenarioUuid)

        val hash = scenarioUuid.hashCode()
        val total = 3
        val expectedIndex = Math.floorMod(hash, total)
        val wrongIndex = (expectedIndex + 1) % total

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // When & Then
        val result = method.invoke(worker, key, wrongIndex, total) as Boolean
        assertFalse(result, "Should return false when hash mod does not equal index")
    }

    @Test
    fun `isMyPartition should return false when key is null`() {
        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val result = method.invoke(worker, null, 0, 3) as Boolean
        assertFalse(result, "Should return false when key is null")
    }

    @Test
    fun `getWebScenarioList should return empty list initially`() {
        val scenarios = worker.getWebScenarioList()
        assertTrue(scenarios.isEmpty(), "Should return empty list initially")
    }

    @Test
    fun `partition distribution should be even across instances`() {
        // Given
        val scenarios = (1..100).map {
            WebScenarioKey(
                serviceUuid = UUID.randomUUID(),
                scenarioUuid = UUID.randomUUID()
            )
        }

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val total = 3
        val partitionCounts = IntArray(total)

        // When
        scenarios.forEach { key ->
            for (index in 0 until total) {
                if (method.invoke(worker, key, index, total) as Boolean) {
                    partitionCounts[index]++
                    break
                }
            }
        }

        // Then
        val totalAssigned = partitionCounts.sum()
        assertEquals(100, totalAssigned, "All scenarios should be assigned")

        partitionCounts.forEachIndexed { index, count ->
            assertTrue(count > 0, "Partition $index should have at least some scenarios")
        }
    }

    @Test
    fun `same key should always map to same partition`() {
        val key = WebScenarioKey(
            serviceUuid = UUID.randomUUID(),
            scenarioUuid = UUID.randomUUID()
        )

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val total = 5
        var assignedPartition = -1

        for (index in 0 until total) {
            if (method.invoke(worker, key, index, total) as Boolean) {
                assignedPartition = index
                break
            }
        }

        // Verify consistency
        repeat(10) {
            for (index in 0 until total) {
                val result = method.invoke(worker, key, index, total) as Boolean
                if (index == assignedPartition) {
                    assertTrue(result)
                } else {
                    assertFalse(result)
                }
            }
        }
    }

    @Test
    fun `floorMod should handle negative hash values correctly`() {
        // Given: UUID that produces negative hash
        val negativeHashUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
        val key = WebScenarioKey(UUID.randomUUID(), negativeHashUuid)

        val method = WebScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            WebScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val total = 3
        var matchedCount = 0

        // Should match exactly one partition
        for (index in 0 until total) {
            if (method.invoke(worker, key, index, total) as Boolean) {
                matchedCount++
                assertTrue(index >= 0 && index < total, "Index should be within valid range")
            }
        }

        assertEquals(1, matchedCount, "Should match exactly one partition")
    }

    @Test
    fun `ZK_BASE_PATH should be correct for web batch`() {
        val field = WebScenarioBatchWorker::class.java.getDeclaredField("ZK_BASE_PATH")
        field.isAccessible = true
        val basePath = field.get(null) as String

        assertEquals("/web-batch/instances", basePath)
    }
}
