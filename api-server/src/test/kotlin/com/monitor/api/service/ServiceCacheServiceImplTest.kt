package com.monitor.api.service

import com.monitor.api.domain.ServiceEntity
import com.monitor.api.repository.ServiceReactiveRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

@ExtendWith(SpringExtension::class)
class ServiceCacheServiceImplTest {

    private val repository: ServiceReactiveRepository = mock()
    private val service = ServiceCacheServiceImpl(repository)

    @Test
    fun `first call should hit repository and cache result, second call should use cache`() {
        val id = UUID.randomUUID()
        val entity = ServiceEntity(
            uuid = id,
            serviceName = "test-service",
            description = "desc",
            updatedTime = null,
            email = "test@example.com"
        )

        whenever(repository.findById(id)).thenReturn(Mono.just(entity))

        val mono1 = service.findById(id)
        val mono2 = service.findById(id)

        StepVerifier.create(mono1)
            .expectNextMatches { it.uuid == id && it.serviceName == "test-service" }
            .verifyComplete()

        StepVerifier.create(mono2)
            .expectNextMatches { it.uuid == id && it.serviceName == "test-service" }
            .verifyComplete()

        // repository는 딱 한 번만 호출되어야 함 (두 번째는 캐시 사용)
        verify(repository, times(1)).findById(id)
    }
}
