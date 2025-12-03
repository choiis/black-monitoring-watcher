package com.monitor.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@Configuration
@EnableReactiveCassandraRepositories(basePackages = ["com.monitor.api.repository"])
class CassandraConfig