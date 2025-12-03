package com.monitor.api.mimir

import com.monitor.api.service.ServiceCacheService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.xerial.snappy.Snappy
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.UUID

@Component
class MimirMetricPusher(
    private val serviceCacheService: ServiceCacheService,
    private val mimirProperties: MimirProperties
) {

    private val log = LoggerFactory.getLogger(MimirMetricPusher::class.java)

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(mimirProperties.url)
        .build()

    fun pushMetric(
        serviceUuid: UUID?,
        metricName: String,
        value: Double,
        labels: Map<String, String>
    ): Mono<Boolean> {
        return serviceCacheService.findById(serviceUuid)
            .flatMap { service ->
                val tenant = service.uuid.toString()
                Mono.fromCallable { buildPrometheusWriteRequest(metricName, value, labels) }
                    .flatMap { payload -> sendToMimir(tenant, payload) }
            }
            .switchIfEmpty(Mono.just(false))
            .onErrorReturn(false)
    }

    @Throws(IOException::class)
    private fun buildPrometheusWriteRequest(
        metricName: String,
        value: Double,
        labels: Map<String, String>
    ): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()

        val allLabels = LinkedHashMap<String, String>()
        allLabels["__name__"] = metricName
        allLabels.putAll(labels)

        val timestampMs = System.currentTimeMillis()

        byteArrayOutputStream.write(0x0a)

        val tsStream = ByteArrayOutputStream()

        for ((name, valueStr) in allLabels) {
            tsStream.write(0x0a)
            val labelBytes = encodeLabel(name, valueStr)
            writeValue(tsStream, labelBytes.size.toLong())
            tsStream.write(labelBytes)
        }

        tsStream.write(0x12)
        val sampleBytes = encodeSample(value, timestampMs)
        writeValue(tsStream, sampleBytes.size.toLong())
        tsStream.write(sampleBytes)

        val tsBytes = tsStream.toByteArray()
        writeValue(byteArrayOutputStream, tsBytes.size.toLong())
        byteArrayOutputStream.write(tsBytes)

        return byteArrayOutputStream.toByteArray()
    }

    @Throws(IOException::class)
    private fun encodeLabel(name: String, value: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()

        byteArrayOutputStream.write(0x0a)
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        writeValue(byteArrayOutputStream, nameBytes.size.toLong())
        byteArrayOutputStream.write(nameBytes)

        byteArrayOutputStream.write(0x12)
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)
        writeValue(byteArrayOutputStream, valueBytes.size.toLong())
        byteArrayOutputStream.write(valueBytes)

        return byteArrayOutputStream.toByteArray()
    }

    @Throws(IOException::class)
    private fun encodeSample(value: Double, timestampMs: Long): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()

        byteArrayOutputStream.write(0x09)
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putDouble(value)
        byteArrayOutputStream.write(buffer.array())

        byteArrayOutputStream.write(0x10)
        writeValue(byteArrayOutputStream, timestampMs)

        return byteArrayOutputStream.toByteArray()
    }

    private fun writeValue(byteArrayOutputStream: ByteArrayOutputStream, value: Long) {
        var v = value
        while (v > 0x7f) {
            byteArrayOutputStream.write(((v and 0x7f) or 0x80).toInt())
            v = v shr 7
        }
        byteArrayOutputStream.write(v.toInt())
    }

    private fun sendToMimir(tenant: String, payload: ByteArray): Mono<Boolean> {
        return try {
            val compressed = Snappy.compress(payload)

            webClient.post()
                .uri("/api/v1/push")
                .header("Content-Type", MediaType.APPLICATION_PROTOBUF_VALUE)
                .header("Content-Encoding", "snappy")
                .header("X-Prometheus-Remote-Write-Version", "0.1.0")
                .header("X-Scope-OrgId", tenant)
                .bodyValue(compressed)
                .retrieve()
                .toBodilessEntity()
                .map { true }
                .onErrorResume { Mono.just(false) }
        } catch (e: IOException) {
            log.warn("Failed to compress payload for tenant={}", tenant, e)
            Mono.just(false)
        }
    }
}
