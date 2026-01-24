package com.monitor.api.repository

import com.monitor.api.domain.ServiceEntity
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ServiceReactiveRepository : ReactiveCassandraRepository<ServiceEntity, UUID>