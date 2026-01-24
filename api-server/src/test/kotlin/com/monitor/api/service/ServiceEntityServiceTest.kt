package com.monitor.api.service

import com.monitor.api.domain.ServiceEntity
import com.monitor.api.repository.ServiceReactiveRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.*

@ExtendWith(SpringExtension::class)
class ServiceEntityServiceTest {

    private val repository: ServiceReactiveRepository = mock()
    private val service = ServiceEntityService(repository)

    @Test
    fun `create should assign uuid if null and set updatedTime`() {
        val entity = ServiceEntity(
            uuid = null,
            serviceName = "svc",
            description = "desc",
            updatedTime = null,
            email = "svc@example.com"
        )

        whenever(repository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ServiceEntity>(0)
            Mono.just(arg)
        }

        StepVerifier.create(service.create(entity))
            .assertNext { saved ->
                assert(saved.uuid != null)
                assert(saved.updatedTime != null)
                assert(saved.serviceName == "svc")
            }
            .verifyComplete()

        verify(repository).save(any())
    }

    @Test
    fun `update should load existing entity and save updated fields`() {
        val id = UUID.randomUUID()
        val existing = ServiceEntity(
            uuid = id,
            serviceName = "old",
            description = "old-desc",
            updatedTime = null,
            email = "old@example.com"
        )

        val input = ServiceEntity(
            uuid = null,
            serviceName = "new",
            description = "new-desc",
            updatedTime = null,
            email = "new@example.com"  // 현재 서비스 코드에서는 email은 업데이트 안 하고 있음
        )

        whenever(repository.findById(id)).thenReturn(Mono.just(existing))
        whenever(repository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ServiceEntity>(0)
            Mono.just(arg)
        }

        StepVerifier.create(service.update(id, input))
            .assertNext { saved ->
                assert(saved.uuid == id)
                assert(saved.serviceName == "new")
                assert(saved.description == "new-desc")
                assert(saved.updatedTime != null)
            }
            .verifyComplete()

        verify(repository).findById(id)
        verify(repository).save(any())
    }

    @Test
    fun `delete should call repository deleteById`() {
        val id = UUID.randomUUID()
        whenever(repository.deleteById(id)).thenReturn(Mono.empty())

        StepVerifier.create(service.delete(id))
            .verifyComplete()

        verify(repository).deleteById(id)
    }
}
