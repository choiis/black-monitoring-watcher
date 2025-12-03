package com.monitor.api.service

import com.datastax.oss.driver.api.core.uuid.Uuids
import com.monitor.api.domain.ApiScenario
import com.monitor.api.domain.ApiScenarioKey
import com.monitor.api.repository.ApiScenarioReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
class ApiScenarioService(
    private val repository: ApiScenarioReactiveRepository
) {

    fun findAll(): Flux<ApiScenario> = repository.findAll()

    fun findByServiceUuid(serviceUuid: UUID): Flux<ApiScenario> =
        repository.findByKeyServiceUuid(serviceUuid)

    fun findOne(serviceUuid: UUID, scenarioUuid: UUID): Mono<ApiScenario> {
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)
        return repository.findById(key)
    }

    fun create(scenario: ApiScenario): Mono<ApiScenario> {
        val now = Instant.now()
        if (scenario.createdTime == null) {
            scenario.createdTime = now
        }
        scenario.updatedTime = now

        if (scenario.key == null) {
            val serviceUuid = Uuids.timeBased()
            val scenarioUuid = Uuids.timeBased()
            scenario.key = ApiScenarioKey(serviceUuid, scenarioUuid)
        } else if (scenario.key?.scenarioUuid == null) {
            scenario.key?.scenarioUuid = Uuids.timeBased()
        }

        return repository.save(scenario)
    }

    fun update(serviceUuid: UUID, scenarioUuid: UUID, scenario: ApiScenario): Mono<ApiScenario> {
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)
        return repository.findById(key)
            .flatMap { existing ->
                existing.serviceName = scenario.serviceName
                existing.url = scenario.url
                existing.method = scenario.method
                existing.headers = scenario.headers
                existing.requestBody = scenario.requestBody
                existing.description = scenario.description
                existing.updatedTime = Instant.now()
                repository.save(existing)
            }
    }

    fun delete(serviceUuid: UUID, scenarioUuid: UUID): Mono<Void> {
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)
        return repository.deleteById(key)
    }
}