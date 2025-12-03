package com.monitor.api.domain

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.CassandraType.Name
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.io.Serializable
import java.util.UUID

@PrimaryKeyClass
data class ApiScenarioKey(
    @PrimaryKeyColumn(name = "service_uuid", type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = Name.UUID)
    var serviceUuid: UUID? = null,

    @PrimaryKeyColumn(name = "scenario_uuid", type = PrimaryKeyType.CLUSTERED)
    @CassandraType(type = Name.UUID)
    var scenarioUuid: UUID? = null
) : Serializable