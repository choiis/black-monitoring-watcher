package com.monitor.api.controller

import com.monitor.api.domain.TcpScenario
import com.monitor.api.service.TcpScenarioService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/v1/tcp-scenarios")
class TcpScenarioController(
    private val service: TcpScenarioService
) {

    @GetMapping
    fun getAll(): Flux<TcpScenario> = service.findAll()

    @GetMapping("/service/{serviceUuid}")
    fun getByService(@PathVariable serviceUuid: UUID): Flux<TcpScenario> =
        service.findByServiceUuid(serviceUuid)

    @GetMapping("/{serviceUuid}/{scenarioUuid}")
    fun getOne(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID
    ): Mono<TcpScenario> = service.findOne(serviceUuid, scenarioUuid)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody scenario: TcpScenario): Mono<TcpScenario> = service.create(scenario)

    @PutMapping("/{serviceUuid}/{scenarioUuid}")
    fun update(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID,
        @RequestBody scenario: TcpScenario
    ): Mono<TcpScenario> = service.update(serviceUuid, scenarioUuid, scenario)

    @DeleteMapping("/{serviceUuid}/{scenarioUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID
    ): Mono<Void> = service.delete(serviceUuid, scenarioUuid)
}