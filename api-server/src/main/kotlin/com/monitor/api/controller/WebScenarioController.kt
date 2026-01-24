package com.monitor.api.controller

import com.monitor.api.domain.WebScenario
import com.monitor.api.service.WebScenarioService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/v1/web-scenarios")
class WebScenarioController(
    private val service: WebScenarioService
) {

    @GetMapping
    fun getAll(): Flux<WebScenario> = service.findAll()

    @GetMapping("/service/{serviceUuid}")
    fun getByService(@PathVariable serviceUuid: UUID): Flux<WebScenario> =
        service.findByServiceUuid(serviceUuid)

    @GetMapping("/{serviceUuid}/{scenarioUuid}")
    fun getOne(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID
    ): Mono<WebScenario> = service.findOne(serviceUuid, scenarioUuid)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody scenario: WebScenario): Mono<WebScenario> = service.create(scenario)

    @PutMapping("/{serviceUuid}/{scenarioUuid}")
    fun update(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID,
        @RequestBody scenario: WebScenario
    ): Mono<WebScenario> = service.update(serviceUuid, scenarioUuid, scenario)

    @DeleteMapping("/{serviceUuid}/{scenarioUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable serviceUuid: UUID,
        @PathVariable scenarioUuid: UUID
    ): Mono<Void> = service.delete(serviceUuid, scenarioUuid)
}
