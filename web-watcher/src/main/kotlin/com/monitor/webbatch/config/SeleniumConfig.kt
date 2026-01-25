package com.monitor.webbatch.config

import io.github.bonigarcia.wdm.WebDriverManager
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class SeleniumConfig(
    @Value("\${selenium.pool-size:3}") val poolSize: Int,
    @Value("\${selenium.headless:true}") private val headless: Boolean
) {

    private val log = LoggerFactory.getLogger(SeleniumConfig::class.java)
    private val driverPool = ConcurrentLinkedQueue<WebDriver>()
    private lateinit var virtualThreadExecutor: ExecutorService

    @Bean
    fun virtualThreadScheduler(): Scheduler {
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()
        log.info("Initialized Virtual Thread Scheduler for Selenium operations")
        return Schedulers.fromExecutorService(virtualThreadExecutor)
    }

    @PreDestroy
    fun shutdownExecutor() {
        if (::virtualThreadExecutor.isInitialized) {
            virtualThreadExecutor.shutdown()
            log.info("Virtual Thread Executor shutdown")
        }
    }

    @PostConstruct
    fun initDriverPool() {
        log.info("Setting up ChromeDriver using WebDriverManager...")
        WebDriverManager.chromedriver().setup()

        log.info("Initializing Selenium WebDriver pool with size: {}", poolSize)
        repeat(poolSize) {
            try {
                val driver = createDriver()
                driverPool.offer(driver)
                log.info("WebDriver instance {} initialized", it + 1)
            } catch (e: Exception) {
                log.error("Failed to initialize WebDriver instance {}", it + 1, e)
            }
        }
    }

    private fun createDriver(): WebDriver {
        val options = ChromeOptions().apply {
            if (headless) {
                addArguments("--headless=new")
            }
            addArguments("--no-sandbox")
            addArguments("--disable-dev-shm-usage")
            addArguments("--disable-gpu")
            addArguments("--window-size=1920,1080")
            addArguments("--disable-extensions")
            addArguments("--disable-infobars")
            addArguments("--remote-allow-origins=*")
            addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
        }
        return ChromeDriver(options)
    }

    fun borrowDriver(): WebDriver? {
        var driver = driverPool.poll()
        if (driver == null) {
            log.warn("No available WebDriver in pool, creating new one")
            try {
                driver = createDriver()
            } catch (e: Exception) {
                log.error("Failed to create WebDriver", e)
                return null
            }
        }
        return driver
    }

    fun returnDriver(driver: WebDriver) {
        try {
            driver.manage().deleteAllCookies()
            driverPool.offer(driver)
        } catch (e: Exception) {
            log.warn("Failed to return driver to pool, closing it", e)
            try {
                driver.quit()
            } catch (ignored: Exception) {
            }
        }
    }

    fun shutdownPool() {
        log.info("Shutting down Selenium WebDriver pool...")
        while (driverPool.isNotEmpty()) {
            val driver = driverPool.poll()
            try {
                driver?.quit()
            } catch (e: Exception) {
                log.warn("Failed to quit WebDriver", e)
            }
        }
    }
}
