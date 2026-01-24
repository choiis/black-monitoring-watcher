package com.monitor.api.service

import com.datastax.oss.driver.api.core.uuid.Uuids
import com.monitor.api.domain.ApiScenario
import com.monitor.api.domain.ApiScenarioKey
import com.monitor.api.repository.ApiScenarioReactiveRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.*

@ExtendWith(SpringExtension::class)
class ApiScenarioServiceTest {

    private val repository: ApiScenarioReactiveRepository = mock()
    private val service = ApiScenarioService(repository)

    @Test
    fun `create should fill key and timestamps and save`() {
        val scenario = ApiScenario(
            key = null,
            serviceName = "api-service",
            url = "http://example.com",
            method = "GET",
            headers = mapOf("X-TEST" to "1"),
            requestBody = null,
            description = "desc",
            createdTime = null,
            updatedTime = null
        )

        whenever(repository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ApiScenario>(0)
            Mono.just(arg)
        }

        StepVerifier.create(service.create(scenario))
            .assertNext { saved ->
                assert(saved.key != null)
                assert(saved.key!!.serviceUuid != null)
                assert(saved.key!!.scenarioUuid != null)
                assert(saved.createdTime != null)
                assert(saved.updatedTime != null)
            }
            .verifyComplete()

        verify(repository).save(any())
    }

    @Test
    fun `update should load existing api scenario and save updated fields`() {
        val serviceUuid = Uuids.timeBased()
        val scenarioUuid = Uuids.timeBased()
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)

        val existing = ApiScenario(
            key = key,
            serviceName = "old-service",
            url = "http://old",
            method = "GET",
            headers = mapOf("h1" to "v1"),
            requestBody = "old-body",
            description = "old",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )

        val input = ApiScenario(
            key = null,
            serviceName = "new-service",
            url = "http://new",
            method = "POST",
            headers = mapOf("h2" to "v2"),
            requestBody = "new-body",
            description = "new",
            createdTime = null,
            updatedTime = null
        )

        whenever(repository.findById(key)).thenReturn(Mono.just(existing))
        whenever(repository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ApiScenario>(0)
            Mono.just(arg)
        }

        StepVerifier.create(service.update(serviceUuid, scenarioUuid, input))
            .assertNext { saved ->
                assert(saved.key == key)
                assert(saved.serviceName == "new-service")
                assert(saved.url == "http://new")
                assert(saved.method == "POST")
                assert(saved.headers == mapOf("h2" to "v2"))
                assert(saved.requestBody == "new-body")
                assert(saved.description == "new")
                assert(saved.updatedTime != null)
            }
            .verifyComplete()

        verify(repository).findById(key)
        verify(repository).save(any())
    }

    @Test
    fun `delete should call repository deleteById`() {
        val serviceUuid = UUID.randomUUID()
        val scenarioUuid = UUID.randomUUID()
        val key = ApiScenarioKey(serviceUuid, scenarioUuid)

        whenever(repository.deleteById(key)).thenReturn(Mono.empty())

        StepVerifier.create(service.delete(serviceUuid, scenarioUuid))
            .verifyComplete()

        verify(repository).deleteById(key)
    }
}
