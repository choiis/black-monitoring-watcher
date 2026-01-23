package com.monitor.api.domain

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

@Table("web_scenario")
data class WebScenario(
    @PrimaryKey
    var key: WebScenarioKey? = null,

    @Column("service_name")
    var serviceName: String? = null,

    @Column("url")
    var url: String? = null,

    @Column("method")
    var method: String? = null,

    @Column("javascript")
    var javascript: String? = null,

    @Column("loginscript")
    var loginScript: String? = null,

    @Column("description")
    var description: String? = null,

    @Column("created_time")
    var createdTime: Instant? = null,

    @Column("updated_time")
    var updatedTime: Instant? = null
)
