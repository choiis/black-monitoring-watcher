package com.monitor.api.domain

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

@Table("tcp_scenario")
data class TcpScenario(
    @PrimaryKey
    var key: TcpScenarioKey? = null,

    @Column("service_name")
    var serviceName: String? = null,

    @Column("ip")
    var ip: String? = null,

    @Column("port")
    var port: Int? = null,

    @Column("description")
    var description: String? = null,

    @Column("created_time")
    var createdTime: Instant? = null,

    @Column("updated_time")
    var updatedTime: Instant? = null
)