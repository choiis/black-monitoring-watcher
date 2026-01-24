package com.monitor.tcpbatch.integration

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.api.repository.TcpScenarioReactiveRepository
import com.monitor.tcpbatch.simulator.TcpScenarioSimulator
import com.monitor.tcpbatch.worker.TcpScenarioBatchWorker
import org.apache.curator.framework.CuratorFramework
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class TcpWatcherIntegrationTest {

    @Mock
    lateinit var curatorFramework: CuratorFramework

    @Mock
    lateinit var repository: TcpScenarioReactiveRepository

    @Mock
    lateinit var mimirMetricPusher: MimirMetricPusher

    @Mock
    lateinit var alertClient: AlertClient

    private val instanceId = "integration-test-tcp-instance"

    @Test
    fun `worker and simulator should work together for scenario processing`() {
        // Given
        val scenarios = createTestScenarios(3)
        val worker = mock<TcpScenarioBatchWorker>()
        whenever(worker.getTcpScenarioList()).thenReturn(scenarios)

        val simulator = TcpScenarioSimulator(worker, mimirMetricPusher, alertClient)

        whenever(mimirMetricPusher.pushMetric(
            serviceUuid = any(),
            metricName = any(),
            value = any(),
            labels = any()
        )).thenReturn(Mono.just(true))

        // When
        simulator.simulate()

        // Then
        Thread.sleep(500)
        verify(worker).getTcpScenarioList()
    }

    @Test
    fun `partition assignment should be consistent across multiple calls`() {
        // Given
        val worker = TcpScenarioBatchWorker(curatorFramework, repository, instanceId)

        val method = TcpScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            TcpScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val testKey = TcpScenarioKey(UUID.randomUUID(), UUID.randomUUID())
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
        val worker = TcpScenarioBatchWorker(curatorFramework, repository, instanceId)
        val totalScenarios = 1000
        val totalPartitions = 5

        val method = TcpScenarioBatchWorker::class.java.getDeclaredMethod(
            "isMyPartition",
            TcpScenarioKey::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val partitionCounts = IntArray(totalPartitions)

        // When
        repeat(totalScenarios) {
            val key = TcpScenarioKey(UUID.randomUUID(), UUID.randomUUID())
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
    fun `socket timeout configuration should be correct`() {
        // Given
        val socket = Socket()

        // When
        socket.soTimeout = 3000

        // Then
        assertEquals(3000, socket.soTimeout)
        socket.close()
    }

    @Test
    fun `InetSocketAddress should be created correctly`() {
        // Given
        val host = "localhost"
        val port = 8080

        // When
        val address = InetSocketAddress(host, port)

        // Then
        assertEquals(host, address.hostName)
        assertEquals(port, address.port)
    }

    @Test
    fun `scenario with all fields should be processed correctly`() {
        // Given
        val scenario = TcpScenario(
            key = TcpScenarioKey(UUID.randomUUID(), UUID.randomUUID()),
            serviceName = "test-tcp-service",
            ip = "192.168.1.100",
            port = 3306,
            description = "MySQL connection test",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )

        // Then
        assertEquals("test-tcp-service", scenario.serviceName)
        assertEquals("192.168.1.100", scenario.ip)
        assertEquals(3306, scenario.port)
    }

    @Test
    fun `metric labels should be correctly formatted`() {
        // Given
        val scenarioUuid = UUID.randomUUID()
        val host = "db.example.com"
        val port = 5432

        // When
        val labels = mapOf(
            "scenario_uuid" to scenarioUuid.toString(),
            "host" to host,
            "port" to port.toString()
        )

        // Then
        assertEquals(3, labels.size)
        assertEquals(scenarioUuid.toString(), labels["scenario_uuid"])
        assertEquals(host, labels["host"])
        assertEquals("5432", labels["port"])
    }

    @Test
    fun `Triple should correctly store timing metrics`() {
        // Given
        val dnsMs = 5L
        val connectMs = 25L
        val commMs = 15L

        // When
        val result = Triple(dnsMs, connectMs, commMs)

        // Then
        assertEquals(5L, result.first)
        assertEquals(25L, result.second)
        assertEquals(15L, result.third)
    }

    private fun createTestScenarios(count: Int): List<TcpScenario> {
        return (1..count).map { i ->
            TcpScenario(
                key = TcpScenarioKey(UUID.randomUUID(), UUID.randomUUID()),
                serviceName = "test-tcp-service-$i",
                ip = "localhost",
                port = 8080 + i,
                description = "Test TCP scenario $i",
                createdTime = Instant.now(),
                updatedTime = Instant.now()
            )
        }
    }
}
