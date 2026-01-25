plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "black-monitoring-kotlin"

include("api-server", "api-watcher", "tcp-watcher", "web-watcher")
