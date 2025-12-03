package com.monitor.api.service

import com.monitor.api.domain.ServiceEntity
import com.monitor.api.repository.ServiceReactiveRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface ServiceCacheService {
    fun findById(serviceUuid: UUID?): Mono<ServiceEntity>
}

@Service
class ServiceCacheServiceImpl(
    private val serviceRepository: ServiceReactiveRepository
) : ServiceCacheService {

    private data class CacheEntry(
        val value: ServiceEntity,
        val cachedAtMillis: Long
    )

    private val cache: MutableMap<UUID, CacheEntry> = ConcurrentHashMap()

    // Cache TTL: 5 minute
    private val ttlMillis: Long = Duration.ofMinutes(5).toMillis()

    override fun findById(serviceUuid: UUID?): Mono<ServiceEntity> {
        val now = System.currentTimeMillis()

        // 1) Cache call
        val entry = cache[serviceUuid]
        if (entry != null && now - entry.cachedAtMillis <= ttlMillis) {
            return Mono.just(entry.value)
        }

        // 2) After TTL Cassandra Select
        return serviceRepository.findById(serviceUuid!!)
            .doOnNext { entity ->
                cache[serviceUuid] = CacheEntry(
                    value = entity,
                    cachedAtMillis = now
                )
            }
    }
}
