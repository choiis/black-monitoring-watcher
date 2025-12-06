package com.monitor.api.service

import com.monitor.api.utils.MailUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Service
class ScenarioFailAlertService(
    private val mailUtils: MailUtils,
    private val serviceCacheService: ServiceCacheService
) {

    private val log = LoggerFactory.getLogger(ScenarioFailAlertService::class.java)

    fun notifyScenarioFailed(
        serviceUuid: UUID,
        scenarioName: String
    ): Mono<Void> {
        return serviceCacheService.findById(serviceUuid)
            .flatMap { serviceEntity ->
                val serviceName = serviceEntity.serviceName ?: "unknown-service"
                val receiver = serviceEntity.email

                val action: Mono<Void> =
                    if (receiver.isNullOrBlank()) {
                        log.warn(
                            "Service {} has no email configured. Skip sending alert. serviceUuid={}",
                            serviceName,
                            serviceUuid
                        )
                        Mono.empty<Void>()
                    } else {
                        val subject = "[Watcher] $serviceName - $scenarioName 실패"
                        val text = """
                        Service: $serviceName
                        Scenario: $scenarioName
                    """.trimIndent()

                        log.info(
                            "Sending scenario fail alert. to={}, subject={}",
                            receiver,
                            subject
                        )

                        Mono.fromRunnable<Void> {
                            mailUtils.sendSimpleMail(
                                to = receiver,
                                subject = subject,
                                text = text
                            )
                        }.subscribeOn(Schedulers.boundedElastic())
                    }

                action
            }
            .then()
    }
}