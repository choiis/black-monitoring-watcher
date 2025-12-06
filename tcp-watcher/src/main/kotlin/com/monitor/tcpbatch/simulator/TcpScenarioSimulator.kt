package com.monitor.tcpbatch.simulator

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.TcpScenario
import com.monitor.api.dto.AlertRequest
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.tcpbatch.worker.TcpScenarioBatchWorker
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

@Component
class TcpScenarioSimulator(
    private val batchWorker: TcpScenarioBatchWorker,
    private val mimirMetricPusher: MimirMetricPusher,
    private val alertClient: AlertClient
) {

    private val log = LoggerFactory.getLogger(TcpScenarioSimulator::class.java)

    @Scheduled(fixedDelayString = "60000", initialDelayString = "30000")
    fun simulate() {
        val scenarios = batchWorker.getTcpScenarioList()
        if (scenarios.isEmpty()) return

        Flux.fromIterable(scenarios)
            .flatMap { simulateScenario(it) }
            .onErrorContinue { ex, obj ->
                log.warn("Error while simulating TCP scenario: {}", obj, ex)
            }
            .subscribe()
    }

    private fun simulateScenario(scenario: TcpScenario): Mono<Void> {
        val host = scenario.ip
        val port = scenario.port

        if (host.isNullOrBlank() || port == null) {
            return Mono.empty()
        }

        return Mono.fromCallable {
            var dnsMs = -1L
            var connectMs = -1L
            var commMs = -1L

            try {
                // DNS
                val dnsStart = System.nanoTime()
                InetAddress.getByName(host)
                val dnsEnd = System.nanoTime()
                dnsMs = (dnsEnd - dnsStart) / 1_000_000

                // CONNECT + COMM
                val connectStart = System.nanoTime()
                Socket().use { socket ->
                    socket.soTimeout = 3000
                    socket.connect(InetSocketAddress(host, port), 3000)
                    val connectEnd = System.nanoTime()
                    connectMs = (connectEnd - connectStart) / 1_000_000

                    val commStart = System.nanoTime()
                    try {
                        val out: OutputStream = socket.getOutputStream()
                        out.write("PING".toByteArray(StandardCharsets.UTF_8))
                        out.flush()

                        try {
                            val `in`: InputStream = socket.getInputStream()
                            val buf = ByteArray(256)
                            val read = `in`.read(buf)
                            if (read >= 0) {
                                val resp = String(buf, 0, read, StandardCharsets.UTF_8)
                                log.debug("TCP response from {}:{} = {}", host, port, resp)
                            }
                        } catch (ste: SocketTimeoutException) {
                            log.debug("No response (read timeout) from {}:{}", host, port)
                        }
                    } catch (ioEx: Exception) {
                        log.debug(
                            "I/O error during TCP communication to {}:{} - {}",
                            host, port, ioEx.toString()
                        )
                    }
                    val commEnd = System.nanoTime()
                    commMs = (commEnd - commStart) / 1_000_000
                }

                log.info(
                    "TCP scenario: host={}, port={}, dnsMs={}, connectMs={}, commMs={}",
                    host,
                    port,
                    dnsMs,
                    connectMs,
                    commMs
                )
            } catch (e: Exception) {
                log.warn(
                    "Failed to simulate TCP scenario for {}:{} - {}",
                    host,
                    port,
                    e.toString()
                )
            }

            Triple(dnsMs, connectMs, commMs)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { ex ->
                log.warn(
                    "Failed to simulate TCP scenario for {}:{} - {}",
                    host,
                    port,
                    ex.toString(),
                )

                alertClient.sendAlert(
                    AlertRequest(
                        serviceUuid = scenario.key?.serviceUuid!!,
                        scenarioUuid = scenario.key?.scenarioUuid!!,
                        serviceName = scenario.serviceName ?: "unknown"
                    )
                )
                    .onErrorResume { alertEx ->
                        log.warn(
                            "Failed to send TCP alert: scenarioUuid={}, reason={}",
                            scenario.key?.scenarioUuid,
                            alertEx.toString()
                        )
                        Mono.empty()
                    }
                    .then(Mono.empty())
            }
            .flatMap { (dnsMs, connectMs, commMs) ->
                val labels: Map<String, String> = mapOf(
                    "scenario_uuid" to (scenario.key?.scenarioUuid?.toString() ?: ""),
                    "host" to host,
                    "port" to port.toString()
                )
                val serviceUuid = scenario.key?.serviceUuid

                val metricMonos = mutableListOf<Mono<Boolean>>()

                if (dnsMs >= 0) {
                    metricMonos += mimirMetricPusher.pushMetric(
                        serviceUuid = serviceUuid,
                        metricName = "black_monitoring_tcp_dns_ms",
                        value = dnsMs.toDouble(),
                        labels = labels
                    )
                }
                if (connectMs >= 0) {
                    metricMonos += mimirMetricPusher.pushMetric(
                        serviceUuid = serviceUuid,
                        metricName = "black_monitoring_tcp_connect_ms",
                        value = connectMs.toDouble(),
                        labels = labels
                    )
                }
                if (commMs >= 0) {
                    metricMonos += mimirMetricPusher.pushMetric(
                        serviceUuid = serviceUuid,
                        metricName = "black_monitoring_tcp_comm_ms",
                        value = commMs.toDouble(),
                        labels = labels
                    )
                }

                val result: Mono<Void> =
                    if (metricMonos.isEmpty()) {
                        Mono.empty()
                    } else {
                        Mono.`when`(metricMonos)
                    }

                result
            }
    }
}
