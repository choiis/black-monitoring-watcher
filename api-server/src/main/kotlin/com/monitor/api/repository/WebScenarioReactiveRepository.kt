package com.monitor.api.repository

import com.monitor.api.domain.WebScenario
import com.monitor.api.domain.WebScenarioKey
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.*

@Repository
interface WebScenarioReactiveRepository : ReactiveCassandraRepository<WebScenario, WebScenarioKey> {

    fun findByKeyServiceUuid(serviceUuid: UUID): Flux<WebScenario>
}
