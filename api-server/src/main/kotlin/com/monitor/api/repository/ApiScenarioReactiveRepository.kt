package com.monitor.api.repository

import com.monitor.api.domain.ApiScenario
import com.monitor.api.domain.ApiScenarioKey
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
interface ApiScenarioReactiveRepository : ReactiveCassandraRepository<ApiScenario, ApiScenarioKey> {

    fun findByKeyServiceUuid(serviceUuid: UUID): Flux<ApiScenario>
}