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

    // Cache TTL: 5 minutes
    private val ttlMillis: Long = Duration.ofMinutes(5).toMillis()

    override fun findById(serviceUuid: UUID?): Mono<ServiceEntity> {
        return Mono.defer {
            val id = serviceUuid ?: return@defer Mono.empty<ServiceEntity>()

            val now = System.currentTimeMillis()
            val entry = cache[id]

            if (entry != null && now - entry.cachedAtMillis <= ttlMillis) {
                return@defer Mono.just(entry.value)
            }

            serviceRepository.findById(id)
                .doOnNext { entity ->
                    cache[id] = CacheEntry(
                        value = entity,
                        cachedAtMillis = System.currentTimeMillis()
                    )
                }
        }
    }
}