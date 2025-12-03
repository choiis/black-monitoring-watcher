package com.monitor.api.domain

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

@Table("api_scenario")
data class ApiScenario(
    @PrimaryKey
    var key: ApiScenarioKey? = null,

    @Column("service_name")
    var serviceName: String? = null,

    @Column("url")
    var url: String? = null,

    @Column("method")
    var method: String? = null,

    @Column("headers")
    var headers: Map<String, String>? = null,

    @Column("requestbody")
    var requestBody: String? = null,

    @Column("description")
    var description: String? = null,

    @Column("created_time")
    var createdTime: Instant? = null,

    @Column("updated_time")
    var updatedTime: Instant? = null
)