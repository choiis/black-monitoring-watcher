package com.monitor.apibatch.worker

import com.monitor.api.domain.ApiScenarioKey
import com.monitor.api.repository.ApiScenarioReactiveRepository
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CreateBuilder
import org.apache.curator.framework.api.DeleteBuilder
import org.apache.curator.framework.api.ExistsBuilder
import org.apache.curator.framework.api.GetChildrenBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ApiScenarioBatchWorkerTest {

    @Mock
    lateinit var curatorFramework: CuratorFramework

    @Mock
    lateinit var repository: ApiScenarioReactiveRepository

    @Mock
    lateinit var createBuilder: CreateBuilder

    @Mock
    lateinit var deleteBuilder: DeleteBuilder

    @Mock
    lateinit var existsBuilder: ExistsBuilder

    @Mock
    lateinit var getChildrenBuilder: GetChildrenBuilder

    private val instanceId = "test-instance-001"

    private lateinit var worker: ApiScenarioBatchWorker

    @BeforeEach
    fun setUp() {
        worker = ApiScenarioBatchWorker(curatorFramework, repository, instanceId)
    }

    @Test
    fun `isMyPartition should return true when hash mod equals index`() {
        // Given: UUID that hashes to a specific partition
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)

        // Calculate expected partition
        val hash = scenarioUuid.hashCode()
        val total = 3
        val expectedIndex = Math.floorMod(hash, total)

        // Use reflection to test private method
        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
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
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)

        val hash = scenarioUuid.hashCode()
        val total = 3
        val expectedIndex = Math.floorMod(hash, total)
        val wrongIndex = (expectedIndex + 1) % total

        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
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
        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val result = method.invoke(worker, null, 0, 3) as Boolean
        assertFalse(result, "Should return false when key is null")
    }

    @Test
    fun `getApiScenarioList should return empty list initially`() {
        val scenarios = worker.getApiScenarioList()
        assertTrue(scenarios.isEmpty(), "Should return empty list initially")
    }

    @Test
    fun `partition distribution should be even across instances`() {
        // Given: Multiple scenarios
        val scenarios = (1..100).map { i ->
            ApiScenarioKey(
                serviceUuid = UUID.randomUUID(),
                scenarioUuid = UUID.randomUUID()
            )
        }

        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val total = 3
        val partitionCounts = IntArray(total)

        // When: Count scenarios per partition
        scenarios.forEach { key ->
            for (index in 0 until total) {
                if (method.invoke(worker, key, index, total) as Boolean) {
                    partitionCounts[index]++
                    break
                }
            }
        }

        // Then: Each partition should have roughly equal scenarios (within reasonable variance)
        val totalAssigned = partitionCounts.sum()
        assertEquals(100, totalAssigned, "All scenarios should be assigned to a partition")

        // Each partition should have at least some scenarios (probabilistic check)
        partitionCounts.forEachIndexed { index, count ->
            assertTrue(count > 0, "Partition $index should have at least some scenarios")
        }
    }

    @Test
    fun `same key should always map to same partition`() {
        val key = ApiScenarioKey(
            serviceUuid = UUID.randomUUID(),
            scenarioUuid = UUID.randomUUID()
        )

        val method = ApiScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            ApiScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val total = 5

        // Find which partition this key belongs to
        var assignedPartition = -1
        for (index in 0 until total) {
            if (method.invoke(worker, key, index, total) as Boolean) {
                assignedPartition = index
                break
            }
        }

        // Verify it consistently maps to the same partition
        repeat(10) {
            for (index in 0 until total) {
                val result = method.invoke(worker, key, index, total) as Boolean
                if (index == assignedPartition) {
                    assertTrue(result, "Key should consistently map to partition $assignedPartition")
                } else {
                    assertFalse(result, "Key should not map to partition $index")
                }
            }
        }
    }
}
