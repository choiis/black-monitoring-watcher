package com.monitor.api.service


import com.monitor.api.domain.ServiceEntity
import com.monitor.api.utils.MailUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ScenarioFailAlertServiceTest(

) {

    private lateinit var mailUtils: MailUtils
    private lateinit var serviceCacheService: ServiceCacheService
    private lateinit var scenarioFailAlertService: ScenarioFailAlertService

    @BeforeEach
    fun setUp() {
        mailUtils = org.mockito.Mockito.mock(MailUtils::class.java)
        serviceCacheService = org.mockito.Mockito.mock(ServiceCacheService::class.java)
        scenarioFailAlertService = ScenarioFailAlertService(mailUtils, serviceCacheService)
    }

    @Test
    fun `when service has email then mail is sent`() {
        // given
        val serviceUuid = UUID.randomUUID()
        val scenarioName = "health-check-scenario"

        val serviceEntity = ServiceEntity(
            uuid = serviceUuid,
            serviceName = "user-service",
            email = "owner@example.com"
        )

        whenever(serviceCacheService.findById(serviceUuid))
            .thenReturn(Mono.just(serviceEntity))

        // when
        val result = scenarioFailAlertService.notifyScenarioFailed(serviceUuid, scenarioName)

        // then
        StepVerifier.create(result)
            .verifyComplete()

        verify(mailUtils).sendSimpleMail(
            eq("owner@example.com"),
            eq("[Watcher] user-service - $scenarioName 실패"),
            anyString(),
            anyString()
        )
    }

    @Test
    fun `when service has no email then mail is not sent`() {
        // given
        val serviceUuid = UUID.randomUUID()
        val scenarioName = "no-email-scenario"

        val serviceEntity = ServiceEntity(
            uuid = serviceUuid,
            serviceName = "no-email-service",
            email = null
        )

        whenever(serviceCacheService.findById(serviceUuid))
            .thenReturn(Mono.just(serviceEntity))

        // when
        val result = scenarioFailAlertService.notifyScenarioFailed(serviceUuid, scenarioName)

        // then
        StepVerifier.create(result)
            .verifyComplete()

        verify(mailUtils, never()).sendSimpleMail(any(), any(), any(), any())
    }

    @Test
    fun `when service not found then completes without sending mail`() {
        // given
        val serviceUuid = UUID.randomUUID()
        val scenarioName = "missing-service-scenario"

        whenever(serviceCacheService.findById(serviceUuid))
            .thenReturn(Mono.empty())

        // when
        val result = scenarioFailAlertService.notifyScenarioFailed(serviceUuid, scenarioName)

        // then
        StepVerifier.create(result)
            .verifyComplete()

        verify(mailUtils, never()).sendSimpleMail(any(), any(), any(), any())
    }
}