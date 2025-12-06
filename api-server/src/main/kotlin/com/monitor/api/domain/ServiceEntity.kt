package com.monitor.api.domain

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("service")
data class ServiceEntity(
    @PrimaryKey
    var uuid: UUID? = null,

    @Column("service_name")
    var serviceName: String? = null,

    @Column("description")
    var description: String? = null,

    @Column("updated_time")
    var updatedTime: Instant? = null,

    @Column("email")
    var email: String? = null
)