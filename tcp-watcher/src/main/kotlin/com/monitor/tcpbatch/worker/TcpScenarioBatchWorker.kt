package com.monitor.tcpbatch.worker

import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import com.monitor.api.repository.TcpScenarioReactiveRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.CuratorCache
import org.apache.curator.framework.recipes.cache.CuratorCacheListener
import org.apache.zookeeper.CreateMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Component
class TcpScenarioBatchWorker(
    private val curatorFramework: CuratorFramework,
    private val repository: TcpScenarioReactiveRepository,
    @Value("\${instance.id:\${random.uuid}}") private val instanceId: String
) {

    companion object {
        private const val ZK_BASE_PATH = "/tcp-batch/instances"
        private val logger = LoggerFactory.getLogger(TcpScenarioBatchWorker::class.java)
    }

    private val tcpScenarioListRef: AtomicReference<List<TcpScenario>> =
        AtomicReference(emptyList())

    private lateinit var curatorCache: CuratorCache

    @PostConstruct
    fun register() {
        curatorFramework.createContainers(ZK_BASE_PATH)
        val path = "$ZK_BASE_PATH/$instanceId"

        if (curatorFramework.checkExists().forPath(path) != null) {
            curatorFramework.delete().forPath(path)
        }

        curatorFramework.create()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path)

        // ZK 인스턴스 리스트 변경 감지용 CuratorCache 설정
        curatorCache = CuratorCache.build(curatorFramework, ZK_BASE_PATH)
        curatorCache.listenable().addListener(
            CuratorCacheListener { _, _, _ ->
                logger.info("Instance change detected, reloading TCP scenarios...")
                reloadScenarios()
            }
        )
        curatorCache.start()
        logger.info("Registered TCP batch instance: $instanceId")
    }

    @PreDestroy
    fun cleanup() {
        if (::curatorCache.isInitialized) {
            curatorCache.close()
            logger.info("CuratorCache closed for TCP instance: $instanceId")
        }
    }

    private fun reloadScenarios() {
        try {
            val instances = curatorFramework.getChildren().forPath(ZK_BASE_PATH) ?: return
            if (instances.isEmpty()) return

            // 인스턴스 목록 정렬해서 index 안정화
            Collections.sort(instances)
            val index = instances.indexOf(instanceId)
            if (index < 0) return
            val total = instances.size

            val myScenarios = repository.findAll()
                .filter { scenario -> isMyPartition(scenario.key, index, total) }
                .collectList()
                .onErrorResume { Mono.just(emptyList()) }
                .block() ?: emptyList()

            tcpScenarioListRef.set(Collections.unmodifiableList(myScenarios))
            logger.info("Reloaded TCP scenarios: count=${myScenarios.size}, index=$index, total=$total")
        } catch (e: Exception) {
            logger.error("Failed to reload TCP scenarios", e)
        }
    }

    @Scheduled(fixedDelayString = "300000", initialDelayString = "30000")
    fun runBatch() {
        reloadScenarios()
    }

    private fun isMyPartition(key: TcpScenarioKey?, index: Int, total: Int): Boolean {
        val uuid: UUID = key?.scenarioUuid ?: return false
        val hash = uuid.hashCode()
        // 음수 방지용 floorMod
        val mod = Math.floorMod(hash, total)
        return mod == index
    }

    fun getTcpScenarioList(): List<TcpScenario> = tcpScenarioListRef.get()
}
