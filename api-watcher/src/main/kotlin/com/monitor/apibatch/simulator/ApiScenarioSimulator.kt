package com.monitor.apibatch.simulator

import com.monitor.apibatch.worker.ApiScenarioBatchWorker
import com.monitor.api.domain.ApiScenario
import com.monitor.api.mimir.MimirMetricPusher
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetAddress
import java.net.URI
import java.time.Duration

@Component
class ApiScenarioSimulator(
    private val batchWorker: ApiScenarioBatchWorker,
    private val webClient: WebClient,
    private val mimirMetricPusher: MimirMetricPusher
) {

    private val log = LoggerFactory.getLogger(ApiScenarioSimulator::class.java)

    @Scheduled(fixedDelayString = "60000", initialDelayString = "30000")
    fun simulate() {
        val scenarios = batchWorker.getApiScenarioList()
        if (scenarios.isEmpty()) return

        Flux.fromIterable(scenarios)
            .flatMap { simulateScenario(it) }
            .onErrorContinue { ex, obj ->
                log.warn("Error while simulating API scenario: {}", obj, ex)
            }
            .subscribe()
    }

    private fun simulateScenario(scenario: ApiScenario): Mono<Void> {
        val url = scenario.url
        val methodStr = scenario.method
        val headers = scenario.headers
        val body = scenario.requestBody

        if (url.isNullOrBlank() || methodStr.isNullOrBlank()) {
            return Mono.empty()
        }

        val method = try {
            HttpMethod.valueOf(methodStr.uppercase())
        } catch (ex: IllegalArgumentException) {
            log.warn("Unsupported HTTP method '{}' for scenario: {}", methodStr, scenario)
            return Mono.empty()
        }

        val uri = try {
            URI.create(url)
        } catch (ex: IllegalArgumentException) {
            log.warn("Invalid URL '{}' for scenario: {}", url, scenario)
            return Mono.empty()
        }

        val host = uri.host
        if (host == null) {
            log.warn("No host in URL '{}' for scenario: {}", url, scenario)
            return Mono.empty()
        }

        val dnsTimeMono: Mono<Long> = Mono.fromCallable {
            val start = System.nanoTime()
            InetAddress.getByName(host)
            val end = System.nanoTime()
            (end - start) / 1_000_000
        }.subscribeOn(Schedulers.boundedElastic())

        return dnsTimeMono.flatMap { dnsMs ->
            val apiStart = System.nanoTime()

            webClient.method(method)
                .uri(uri)
                .headers { h ->
                    headers?.forEach { (k, v) -> h.add(k, v) }
                }
                .body(if (body != null) BodyInserters.fromValue(body) else BodyInserters.empty())
                .exchangeToMono { response ->
                    val status = response.statusCode().value()

                    response.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .flatMap { respBody ->
                            val apiEnd = System.nanoTime()
                            val apiMs = (apiEnd - apiStart) / 1_000_000

                            log.info(
                                "API scenario: method={}, url={}, status={}, dnsMs={}, apiMs={}, bodySize={}",
                                method,
                                url,
                                status,
                                dnsMs,
                                apiMs,
                                respBody.length
                            )

                            val labels: Map<String, String> = mapOf(
                                "scenario_uuid" to (scenario.key?.scenarioUuid?.toString() ?: ""),
                                "url" to url,
                                "method" to method.toString(),
                                "status" to status.toString()
                            )

                            val serviceUuid = scenario.key?.serviceUuid

                            // 메트릭 2개: DNS, API 요청 시간
                            Mono.zip(
                                mimirMetricPusher.pushMetric(
                                    serviceUuid,
                                    "black_monitoring_api_dns_ms",
                                    dnsMs.toDouble(),
                                    labels
                                ),
                                mimirMetricPusher.pushMetric(
                                    serviceUuid = serviceUuid,
                                    metricName = "black_monitoring_api_request_ms",
                                    value = apiMs.toDouble(),
                                    labels = labels
                                )
                            ).then()

                        }
                }
                .timeout(Duration.ofSeconds(10))
        }.then()
    }
}