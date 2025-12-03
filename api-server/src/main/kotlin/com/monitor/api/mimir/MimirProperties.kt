package com.monitor.api.mimir

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "mimir")
class MimirProperties {
    var url: String = "http://localhost:10100"
}