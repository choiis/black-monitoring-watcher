package com.monitor.api.service

import com.datastax.oss.driver.api.core.uuid.Uuids
import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import com.monitor.api.repository.TcpScenarioReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
class TcpScenarioService(
    private val repository: TcpScenarioReactiveRepository
) {

    fun findAll(): Flux<TcpScenario> = repository.findAll()

    fun findByServiceUuid(serviceUuid: UUID): Flux<TcpScenario> =
        repository.findByKeyServiceUuid(serviceUuid)

    fun findOne(serviceUuid: UUID, scenarioUuid: UUID): Mono<TcpScenario> {
        val key = TcpScenarioKey(serviceUuid, scenarioUuid)
        return repository.findById(key)
    }

    fun create(scenario: TcpScenario): Mono<TcpScenario> {
        val now = Instant.now()
        if (scenario.createdTime == null) {
            scenario.createdTime = now
        }
        scenario.updatedTime = now

        if (scenario.key == null) {
            val serviceUuid = Uuids.timeBased()
            val scenarioUuid = Uuids.timeBased()
            scenario.key = TcpScenarioKey(serviceUuid, scenarioUuid)
        } else if (scenario.key?.scenarioUuid == null) {
            scenario.key?.scenarioUuid = Uuids.timeBased()
        }

        return repository.save(scenario)
    }

    fun update(serviceUuid: UUID, scenarioUuid: UUID, scenario: TcpScenario): Mono<TcpScenario> {
        val key = TcpScenarioKey(serviceUuid, scenarioUuid)
        return repository.findById(key)
            .flatMap { existing ->
                existing.serviceName = scenario.serviceName
                existing.ip = scenario.ip
                existing.port = scenario.port
                existing.description = scenario.description
                existing.updatedTime = Instant.now()
                repository.save(existing)
            }
    }

    fun delete(serviceUuid: UUID, scenarioUuid: UUID): Mono<Void> {
        val key = TcpScenarioKey(serviceUuid, scenarioUuid)
        return repository.deleteById(key)
    }
}