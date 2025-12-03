package com.monitor.api.controller

import com.monitor.api.domain.ServiceEntity
import com.monitor.api.service.ServiceEntityService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v1/services")
class ServiceController(
    private val service: ServiceEntityService
) {

    @GetMapping
    fun getAll(): Flux<ServiceEntity> = service.findAll()

    @GetMapping("/{uuid}")
    fun getOne(@PathVariable uuid: UUID): Mono<ServiceEntity> = service.findOne(uuid)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody entity: ServiceEntity): Mono<ServiceEntity> = service.create(entity)

    @PutMapping("/{uuid}")
    fun update(@PathVariable uuid: UUID, @RequestBody entity: ServiceEntity): Mono<ServiceEntity> =
        service.update(uuid, entity)

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable uuid: UUID): Mono<Void> = service.delete(uuid)
}