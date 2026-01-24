package com.monitor.api.service

import com.datastax.oss.driver.api.core.uuid.Uuids
import com.monitor.api.domain.WebScenario
import com.monitor.api.domain.WebScenarioKey
import com.monitor.api.repository.WebScenarioReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Service
class WebScenarioService(
    private val repository: WebScenarioReactiveRepository
) {

    fun findAll(): Flux<WebScenario> = repository.findAll()

    fun findByServiceUuid(serviceUuid: UUID): Flux<WebScenario> =
        repository.findByKeyServiceUuid(serviceUuid)

    fun findOne(serviceUuid: UUID, scenarioUuid: UUID): Mono<WebScenario> {
        val key = WebScenarioKey(serviceUuid, scenarioUuid)
        return repository.findById(key)
    }

    fun create(scenario: WebScenario): Mono<WebScenario> {
        val now = Instant.now()
        if (scenario.createdTime == null) {
            scenario.createdTime = now
        }
        scenario.updatedTime = now

        if (scenario.key == null) {
            val serviceUuid = Uuids.timeBased()
            val scenarioUuid = Uuids.timeBased()
            scenario.key = WebScenarioKey(serviceUuid, scenarioUuid)
        } else if (scenario.key?.scenarioUuid == null) {
            scenario.key?.scenarioUuid = Uuids.timeBased()
        }

        return repository.save(scenario)
    }

    fun update(serviceUuid: UUID, scenarioUuid: UUID, scenario: WebScenario): Mono<WebScenario> {
        val key = WebScenarioKey(serviceUuid, scenarioUuid)
        return repository.findById(key)
            .flatMap { existing ->
                existing.serviceName = scenario.serviceName
                existing.url = scenario.url
                existing.method = scenario.method
                existing.javascript = scenario.javascript
                existing.loginScript = scenario.loginScript
                existing.description = scenario.description
                existing.updatedTime = Instant.now()
                repository.save(existing)
            }
    }

    fun delete(serviceUuid: UUID, scenarioUuid: UUID): Mono<Void> {
        val key = WebScenarioKey(serviceUuid, scenarioUuid)
        return repository.deleteById(key)
    }
}
