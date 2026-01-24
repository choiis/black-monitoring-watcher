package com.monitor.api.mimir

import com.monitor.api.domain.ServiceEntity
import com.monitor.api.service.ServiceCacheService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.*

@ExtendWith(MockitoExtension::class)
class MimirMetricPusherTest {

    @Mock
    lateinit var serviceCacheService: ServiceCacheService

    @Mock
    lateinit var mimirProperties: MimirProperties

    @Mock
    lateinit var webClient: WebClient

    @Mock
    lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec

    @Mock
    lateinit var requestBodySpec: WebClient.RequestBodySpec

    @Mock
    lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>

    @Mock
    lateinit var responseSpec: WebClient.ResponseSpec

    private lateinit var pusher: MimirMetricPusher

    @BeforeEach
    fun setUp() {
        // Mimir URL 설정
        whenever(mimirProperties.url).thenReturn("http://localhost:10100")

        // 실제 객체 생성
        pusher = MimirMetricPusher(serviceCacheService, mimirProperties)

        // private val webClient 에 mock 주입 (리플렉션 사용)
        val field = MimirMetricPusher::class.java.getDeclaredField("webClient")
        field.isAccessible = true
        field.set(pusher, webClient)
    }

    @Test
    fun `pushMetric should return true when service exists and Mimir responds OK`() {
        // given
        val serviceUuid = UUID.randomUUID()
        val serviceEntity = ServiceEntity(
            uuid = serviceUuid,
            serviceName = "test-service",
            description = "desc",
            updatedTime = null,
            email = "test@example.com"
        )

        whenever(serviceCacheService.findById(serviceUuid)).thenReturn(Mono.just(serviceEntity))

        // WebClient 체인 mocking
        whenever(webClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri("/api/v1/push")).thenReturn(requestBodySpec)
        whenever(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.toBodilessEntity()).thenReturn(
            Mono.just(ResponseEntity.ok().build())
        )

        val labels = mapOf("scenario_uuid" to serviceUuid.toString())

        // when
        val result = pusher.pushMetric(
            serviceUuid = serviceUuid,
            metricName = "black_monitoring_api_request_ms",
            value = 123.4,
            labels = labels
        )

        // then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete()

        verify(serviceCacheService).findById(serviceUuid)
        verify(webClient).post()
    }

    @Test
    fun `pushMetric should return false when service not found`() {
        // given
        val serviceUuid = UUID.randomUUID()
        whenever(serviceCacheService.findById(serviceUuid)).thenReturn(Mono.empty())

        val labels = emptyMap<String, String>()

        // when
        val result = pusher.pushMetric(
            serviceUuid = serviceUuid,
            metricName = "black_monitoring_api_dns_ms",
            value = 10.0,
            labels = labels
        )

        // then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete()

        verify(serviceCacheService).findById(serviceUuid)
    }
}
