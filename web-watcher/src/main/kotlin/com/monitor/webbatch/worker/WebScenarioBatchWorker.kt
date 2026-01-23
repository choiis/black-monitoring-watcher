package com.monitor.webbatch.worker

import com.monitor.api.domain.WebScenario
import com.monitor.api.domain.WebScenarioKey
import com.monitor.api.repository.WebScenarioReactiveRepository
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
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class WebScenarioBatchWorker(
    private val curatorFramework: CuratorFramework,
    private val repository: WebScenarioReactiveRepository,
    @Value("\${instance.id:\${random.uuid}}") private val instanceId: String
) {

    companion object {
        private const val ZK_BASE_PATH = "/web-batch/instances"
        private val logger = LoggerFactory.getLogger(WebScenarioBatchWorker::class.java)
    }

    private val webScenarioListRef: AtomicReference<List<WebScenario>> =
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

        curatorCache = CuratorCache.build(curatorFramework, ZK_BASE_PATH)
        curatorCache.listenable().addListener(
            CuratorCacheListener { _, _, _ ->
                logger.info("Instance change detected, reloading Web scenarios...")
                reloadScenarios()
            }
        )
        curatorCache.start()
        logger.info("Registered Web batch instance: $instanceId")
    }

    @PreDestroy
    fun cleanup() {
        if (::curatorCache.isInitialized) {
            curatorCache.close()
            logger.info("CuratorCache closed for Web instance: $instanceId")
        }
    }

    private fun reloadScenarios() {
        try {
            val instances = curatorFramework.getChildren().forPath(ZK_BASE_PATH) ?: return
            if (instances.isEmpty()) return

            Collections.sort(instances)
            val index = instances.indexOf(instanceId)
            if (index < 0) return
            val total = instances.size

            val myScenarios = repository.findAll()
                .filter { scenario -> isMyPartition(scenario.key, index, total) }
                .collectList()
                .onErrorResume { Mono.just(emptyList()) }
                .block() ?: emptyList()

            webScenarioListRef.set(Collections.unmodifiableList(myScenarios))
            logger.info("Reloaded Web scenarios: count=${myScenarios.size}, index=$index, total=$total")
        } catch (e: Exception) {
            logger.error("Failed to reload Web scenarios", e)
        }
    }

    @Scheduled(fixedDelayString = "300000", initialDelayString = "30000")
    fun runBatch() {
        reloadScenarios()
    }

    private fun isMyPartition(key: WebScenarioKey?, index: Int, total: Int): Boolean {
        val uuid: UUID = key?.scenarioUuid ?: return false
        val hash = uuid.hashCode()
        val mod = Math.floorMod(hash, total)
        return mod == index
    }

    fun getWebScenarioList(): List<WebScenario> = webScenarioListRef.get()
}
