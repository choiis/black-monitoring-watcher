package com.monitor.tcpbatch.config

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ZookeeperConfig {

    @Bean
    fun curatorFramework(
        @Value("\${zookeeper.connect-string:localhost:2181}") connectString: String
    ): CuratorFramework {
        val client = CuratorFrameworkFactory.newClient(
            connectString,
            ExponentialBackoffRetry(1000, 3)
        )
        client.start()
        return client
    }
}