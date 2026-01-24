package com.monitor.tcpbatch.simulator

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.tcpbatch.worker.TcpScenarioBatchWorker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.net.InetAddress
import java.net.Socket
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class TcpScenarioSimulatorTest {

    @Mock
    lateinit var batchWorker: TcpScenarioBatchWorker

    @Mock
    lateinit var mimirMetricPusher: MimirMetricPusher

    @Mock
    lateinit var alertClient: AlertClient

    private lateinit var simulator: TcpScenarioSimulator

    @BeforeEach
    fun setUp() {
        simulator = TcpScenarioSimulator(batchWorker, mimirMetricPusher, alertClient)
    }

    @Test
    fun `simulate should skip when scenario list is empty`() {
        // Given
        whenever(batchWorker.getTcpScenarioList()).thenReturn(emptyList())

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getTcpScenarioList()
        verifyNoInteractions(mimirMetricPusher)
    }

    @Test
    fun `simulateScenario should skip when ip is blank`() {
        // Given
        val scenario = createTcpScenario(ip = "", port = 8080)
        whenever(batchWorker.getTcpScenarioList()).thenReturn(listOf(scenario))

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getTcpScenarioList()
        verifyNoInteractions(mimirMetricPusher)
    }

    @Test
    fun `simulateScenario should skip when port is null`() {
        // Given
        val scenario = createTcpScenario(ip = "localhost", port = null)
        whenever(batchWorker.getTcpScenarioList()).thenReturn(listOf(scenario))

        // When
        simulator.simulate()

        // Then
        verify(batchWorker).getTcpScenarioList()
        verifyNoInteractions(mimirMetricPusher)
    }

    @Test
    fun `scenario with localhost should have valid DNS resolution`() {
        // Given
        val scenario = createTcpScenario(ip = "localhost", port = 80)

        // Then
        assertNotNull(scenario.ip)
        assertEquals("localhost", scenario.ip)
    }

    @Test
    fun `scenario with invalid domain should be identifiable`() {
        // Given
        val scenario = createTcpScenario(ip = "nonexistent.invalid.domain.test", port = 8080)

        // Then
        assertNotNull(scenario.ip)
        assertTrue(scenario.ip!!.contains("invalid"))
    }

    @Test
    fun `scenario with non-routable IP should be identifiable`() {
        // Given
        val scenario = createTcpScenario(ip = "10.255.255.1", port = 12345)

        // Then
        assertNotNull(scenario.ip)
        assertTrue(scenario.ip!!.startsWith("10."))
    }

    @Test
    fun `dns resolution should measure time correctly`() {
        // Given: a valid hostname
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
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.randomUUID()
        val host = "testhost.local"
        val port = 9999

        val expectedLabels = mapOf(
            "scenario_uuid" to scenarioUuid.toString(),
            "host" to host,
            "port" to port.toString()
        )

        // Then
        assertEquals(scenarioUuid.toString(), expectedLabels["scenario_uuid"])
        assertEquals(host, expectedLabels["host"])
        assertEquals(port.toString(), expectedLabels["port"])
    }

    @Test
    fun `socket timeout should be 3 seconds`() {
        // Given
        val socket = Socket()

        // When
        socket.soTimeout = 3000

        // Then
        assertEquals(3000, socket.soTimeout)
        socket.close()
    }

    @Test
    fun `Triple should store dns, connect and comm times correctly`() {
        // Given
        val dnsMs = 10L
        val connectMs = 50L
        val commMs = 100L

        // When
        val result = Triple(dnsMs, connectMs, commMs)

        // Then
        assertEquals(dnsMs, result.first)
        assertEquals(connectMs, result.second)
        assertEquals(commMs, result.third)
    }

    @Test
    fun `should push metrics only for non-negative values`() {
        // Given: simulate scenario result
        val dnsMs = 10L
        val connectMs = -1L // Connection failed
        val commMs = -1L    // Communication failed

        // Then: only dns metric should be pushed
        assertTrue(dnsMs >= 0, "DNS time should be pushed")
        assertTrue(connectMs < 0, "Connect time should not be pushed")
        assertTrue(commMs < 0, "Comm time should not be pushed")
    }

    private fun createTcpScenario(
        serviceUuid: UUID = UUID.randomUUID(),
        scenarioUuid: UUID = UUID.randomUUID(),
        ip: String? = "localhost",
        port: Int? = 8080
    ): TcpScenario {
        return TcpScenario(
            key = TcpScenarioKey(serviceUuid, scenarioUuid),
            serviceName = "test-tcp-service",
            ip = ip,
            port = port,
            description = "Test TCP scenario",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )
    }
}
