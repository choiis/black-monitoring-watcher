import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
}

allprojects {
    group = "com.monitor"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

extra["springCloudVersion"] = "2024.0.0"

subprojects {
    plugins.withType<JavaPlugin> {
        the<JavaPluginExtension>().toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
