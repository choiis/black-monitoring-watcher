package com.monitor.api.service

import com.datastax.oss.driver.api.core.uuid.Uuids
import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import com.monitor.api.repository.TcpScenarioReactiveRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

@ExtendWith(SpringExtension::class)
class TcpScenarioServiceTest {

    private val repository: TcpScenarioReactiveRepository = mock()
    private val service = TcpScenarioService(repository)

    @Test
    fun `create should fill key and timestamps and save`() {
        val scenario = TcpScenario(
            key = null,
            serviceName = "tcp-service",
            ip = "127.0.0.1",
            port = 8080,
            description = "desc",
            createdTime = null,
            updatedTime = null
        )

        whenever(repository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<TcpScenario>(0)
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

        verify(repository, times(1)).save(any())
    }

    @Test
    fun `update should load existing scenario and save updated fields`() {
        val serviceUuid = Uuids.timeBased()
        val scenarioUuid = Uuids.timeBased()
        val key = TcpScenarioKey(serviceUuid, scenarioUuid)

        val existing = TcpScenario(
            key = key,
            serviceName = "old-name",
            ip = "10.0.0.1",
            port = 9000,
            description = "old",
            createdTime = Instant.now(),
            updatedTime = Instant.now()
        )

        val updateInput = TcpScenario(
            key = null,
            serviceName = "new-name",
            ip = "10.0.0.2",
            port = 9001,
            description = "new",
            createdTime = null,
            updatedTime = null
        )

        whenever(repository.findById(key)).thenReturn(Mono.just(existing))
        whenever(repository.save(any())).thenAnswer { invocation ->
            val arg = invocation.getArgument<TcpScenario>(0)
            Mono.just(arg)
        }

        StepVerifier.create(service.update(serviceUuid, scenarioUuid, updateInput))
            .assertNext { saved ->
                assert(saved.key == key)
                assert(saved.serviceName == "new-name")
                assert(saved.ip == "10.0.0.2")
                assert(saved.port == 9001)
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
        val key = TcpScenarioKey(serviceUuid, scenarioUuid)

        whenever(repository.deleteById(key)).thenReturn(Mono.empty())

        StepVerifier.create(service.delete(serviceUuid, scenarioUuid))
            .verifyComplete()

        verify(repository).deleteById(key)
    }
}
