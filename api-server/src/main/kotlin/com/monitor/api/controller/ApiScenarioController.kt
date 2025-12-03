package com.monitor.api.controller

import com.monitor.api.domain.ApiScenario
import com.monitor.api.service.ApiScenarioService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v1/api-scenarios")
class ApiScenarioController(
    private val service: ApiScenarioService
) {

    @GetMapping
    fun getAll(): Flux<ApiScenario> = service.findAll()

    @GetMapping("/service/{serviceUuid}")
    fun getByService(@PathVariable serviceUuid: UUID): Flux<ApiScenario> =
        service.findByServiceUuid(serviceUuid)

    @GetMapping("/{serviceUuid}/{scenarioUuid}")
    fun getOne(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID
    ): Mono<ApiScenario> = service.findOne(serviceUuid, scenarioUuid)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody scenario: ApiScenario): Mono<ApiScenario> = service.create(scenario)

    @PutMapping("/{serviceUuid}/{scenarioUuid}")
    fun update(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID,
        @RequestBody scenario: ApiScenario
    ): Mono<ApiScenario> = service.update(serviceUuid, scenarioUuid, scenario)

    @DeleteMapping("/{serviceUuid}/{scenarioUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID
    ): Mono<Void> = service.delete(serviceUuid, scenarioUuid)
}