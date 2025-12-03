package com.monitor.api.service

import com.datastax.oss.driver.api.core.uuid.Uuids
import com.monitor.api.domain.ServiceEntity
import com.monitor.api.repository.ServiceReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
class ServiceEntityService(
    private val repository: ServiceReactiveRepository
) {

    fun findAll(): Flux<ServiceEntity> = repository.findAll()

    fun findOne(uuid: UUID): Mono<ServiceEntity> = repository.findById(uuid)

    fun create(entity: ServiceEntity): Mono<ServiceEntity> {
        val now = Instant.now()
        if (entity.uuid == null) {
            entity.uuid = Uuids.timeBased()
        }
        entity.updatedTime = now
        return repository.save(entity)
    }

    fun update(uuid: UUID, entity: ServiceEntity): Mono<ServiceEntity> =
        repository.findById(uuid)
            .flatMap { existing ->
                existing.serviceName = entity.serviceName
                existing.description = entity.description
                existing.updatedTime = Instant.now()
                repository.save(existing)
            }

    fun delete(uuid: UUID): Mono<Void> = repository.deleteById(uuid)
}