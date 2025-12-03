package com.monitor.api.repository

import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
interface TcpScenarioReactiveRepository : ReactiveCassandraRepository<TcpScenario, TcpScenarioKey> {

    fun findByKeyServiceUuid(serviceUuid: UUID): Flux<TcpScenario>
}