package com.monitor.webbatch.simulator

import com.monitor.api.client.AlertClient
import com.monitor.api.domain.WebScenario
import com.monitor.api.dto.AlertRequest
import com.monitor.api.mimir.MimirMetricPusher
import com.monitor.webbatch.config.SeleniumConfig
import com.monitor.webbatch.worker.WebScenarioBatchWorker
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetAddress
import java.net.URI
import java.time.Duration

@Component
class WebScenarioSimulator(
    private val batchWorker: WebScenarioBatchWorker,
    private val seleniumConfig: SeleniumConfig,
    private val mimirMetricPusher: MimirMetricPusher,
    private val alertClient: AlertClient
) {

    private val log = LoggerFactory.getLogger(WebScenarioSimulator::class.java)

    @Scheduled(fixedDelayString = "60000", initialDelayString = "30000")
    fun simulate() {
        val scenarios = batchWorker.getWebScenarioList()
        if (scenarios.isEmpty()) return

        Flux.fromIterable(scenarios)
            .flatMap({ simulateScenario(it) }, 1) // concurrency 1 for WebDriver safety
            .onErrorContinue { ex, obj ->
                log.warn("Error while simulating Web scenario: {}", obj, ex)
            }
            .subscribe()
    }

    private fun simulateScenario(scenario: WebScenario): Mono<Void> {
        val url = scenario.url

        if (url.isNullOrBlank()) {
            return Mono.empty()
        }

        val uri = try {
            URI.create(url)
        } catch (ex: IllegalArgumentException) {
            log.warn("Invalid URL '{}' for scenario: {}", url, scenario, ex)
            return Mono.empty()
        }

        val host = uri.host
        if (host == null) {
            log.warn("No host in URL '{}' for scenario: {}", url, scenario)
            return Mono.empty()
        }

        return Mono.fromCallable {
            executeWebScenario(scenario, url, host)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { result ->
                pushMetrics(scenario, url, result)
            }
            .onErrorResume { ex ->
                log.warn(
                    "Web scenario failed: scenarioUuid={}, url={}, reason={}",
                    scenario.key?.scenarioUuid,
                    url,
                    ex.toString()
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
                            "Failed to send Web alert: scenarioUuid={}, reason={}",
                            scenario.key?.scenarioUuid,
                            alertEx.toString()
                        )
                        Mono.empty()
                    }
                    .then(Mono.empty())
            }
    }

    private fun executeWebScenario(scenario: WebScenario, url: String, host: String): WebScenarioResult {
        var dnsMs = -1L
        var pageLoadMs = -1L
        var jsExecMs = -1L
        var loginMs = -1L
        var success = true

        // DNS resolution
        val dnsStart = System.nanoTime()
        try {
            InetAddress.getByName(host)
            dnsMs = (System.nanoTime() - dnsStart) / 1_000_000
        } catch (e: Exception) {
            log.warn("DNS resolution failed for host: {}", host, e)
            success = false
            return WebScenarioResult(dnsMs, pageLoadMs, jsExecMs, loginMs, success)
        }

        val driver: WebDriver? = seleniumConfig.borrowDriver()
        if (driver == null) {
            log.warn("Failed to borrow WebDriver for scenario: {}", scenario.key?.scenarioUuid)
            success = false
            return WebScenarioResult(dnsMs, pageLoadMs, jsExecMs, loginMs, success)
        }

        try {
            // Page load
            val pageLoadStart = System.nanoTime()
            driver.get(url)

            // Wait for page to be ready
            val wait = WebDriverWait(driver, Duration.ofSeconds(30))
            wait.until { webDriver ->
                val jsExecutor = webDriver as JavascriptExecutor
                jsExecutor.executeScript("return document.readyState") == "complete"
            }
            pageLoadMs = (System.nanoTime() - pageLoadStart) / 1_000_000

            // Execute login script if exists
            val loginScript = scenario.loginScript
            if (!loginScript.isNullOrBlank()) {
                val loginStart = System.nanoTime()
                try {
                    val jsExecutor = driver as JavascriptExecutor
                    jsExecutor.executeScript(loginScript)
                    loginMs = (System.nanoTime() - loginStart) / 1_000_000
                    log.debug("Login script executed for scenario: {}", scenario.key?.scenarioUuid)
                } catch (e: Exception) {
                    log.warn("Login script execution failed for scenario: {}", scenario.key?.scenarioUuid, e)
                    loginMs = (System.nanoTime() - loginStart) / 1_000_000
                }
            }

            // Execute javascript if exists
            val javascript = scenario.javascript
            if (!javascript.isNullOrBlank()) {
                val jsStart = System.nanoTime()
                try {
                    val jsExecutor = driver as JavascriptExecutor
                    jsExecutor.executeScript(javascript)
                    jsExecMs = (System.nanoTime() - jsStart) / 1_000_000
                    log.debug("JavaScript executed for scenario: {}", scenario.key?.scenarioUuid)
                } catch (e: Exception) {
                    log.warn("JavaScript execution failed for scenario: {}", scenario.key?.scenarioUuid, e)
                    jsExecMs = (System.nanoTime() - jsStart) / 1_000_000
                }
            }

            log.info(
                "Web scenario: url={}, dnsMs={}, pageLoadMs={}, jsExecMs={}, loginMs={}",
                url,
                dnsMs,
                pageLoadMs,
                jsExecMs,
                loginMs
            )

        } catch (e: Exception) {
            log.warn("Web scenario execution failed for URL: {}", url, e)
            success = false
        } finally {
            seleniumConfig.returnDriver(driver)
        }

        return WebScenarioResult(dnsMs, pageLoadMs, jsExecMs, loginMs, success)
    }

    private fun pushMetrics(scenario: WebScenario, url: String, result: WebScenarioResult): Mono<Void> {
        val labels: Map<String, String> = mapOf(
            "scenario_uuid" to (scenario.key?.scenarioUuid?.toString() ?: ""),
            "url" to url
        )
        val serviceUuid = scenario.key?.serviceUuid

        val metricMonos = mutableListOf<Mono<Boolean>>()

        if (result.dnsMs >= 0) {
            metricMonos += mimirMetricPusher.pushMetric(
                serviceUuid = serviceUuid,
                metricName = "black_monitoring_web_dns_ms",
                value = result.dnsMs.toDouble(),
                labels = labels
            )
        }
        if (result.pageLoadMs >= 0) {
            metricMonos += mimirMetricPusher.pushMetric(
                serviceUuid = serviceUuid,
                metricName = "black_monitoring_web_page_load_ms",
                value = result.pageLoadMs.toDouble(),
                labels = labels
            )
        }
        if (result.jsExecMs >= 0) {
            metricMonos += mimirMetricPusher.pushMetric(
                serviceUuid = serviceUuid,
                metricName = "black_monitoring_web_js_exec_ms",
                value = result.jsExecMs.toDouble(),
                labels = labels
            )
        }
        if (result.loginMs >= 0) {
            metricMonos += mimirMetricPusher.pushMetric(
                serviceUuid = serviceUuid,
                metricName = "black_monitoring_web_login_ms",
                value = result.loginMs.toDouble(),
                labels = labels
            )
        }

        return if (metricMonos.isEmpty()) {
            Mono.empty()
        } else {
            Mono.`when`(metricMonos)
        }
    }

    data class WebScenarioResult(
        val dnsMs: Long,
        val pageLoadMs: Long,
        val jsExecMs: Long,
        val loginMs: Long,
        val success: Boolean
    )
}
