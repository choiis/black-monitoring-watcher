package com.monitor.tcpbatch.worker

import com.monitor.api.domain.TcpScenario
import com.monitor.api.domain.TcpScenarioKey
import com.monitor.api.repository.TcpScenarioReactiveRepository
import jakarta.annotation.PostConstruct
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class TcpScenarioBatchWorker(
    private val curatorFramework: CuratorFramework,
    private val repository: TcpScenarioReactiveRepository,
    @Value("\${instance.id:\${random.uuid}}") private val instanceId: String
) {

    companion object {
        private const val ZK_BASE_PATH = "/tcp-batch/instances"
    }

    private val tcpScenarioListRef: AtomicReference<List<TcpScenario>> =
        AtomicReference(emptyList())

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
    }

    @Scheduled(fixedDelayString = "300000", initialDelayString = "30000")
    fun runBatch() {
        try {
            val instances = curatorFramework.getChildren().forPath(ZK_BASE_PATH) ?: return
            if (instances.isEmpty()) return

            Collections.sort(instances)
            val index = instances.indexOf(instanceId)
            if (index < 0) return
            val total = instances.size

            var myScenarios = repository.findAll()
                .filter { scenario -> isMyPartition(scenario.key, index, total) }
                .collectList()
                .onErrorResume { Mono.just(emptyList()) }
                .block()

            if (myScenarios == null) {
                myScenarios = emptyList()
            }

            tcpScenarioListRef.set(Collections.unmodifiableList(myScenarios))
        } catch (e: Exception) {

        }
    }

    private fun isMyPartition(key: TcpScenarioKey?, index: Int, total: Int): Boolean {
        val uuid: UUID = key?.scenarioUuid ?: return false
        val hash = uuid.hashCode()
        val mod = Math.floorMod(hash, total)
        return mod == index
    }

    fun getTcpScenarioList(): List<TcpScenario> = tcpScenarioListRef.get()
}