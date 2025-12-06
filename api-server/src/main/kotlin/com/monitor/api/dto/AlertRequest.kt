package com.monitor.api.dto

import java.util.UUID

data class AlertRequest(
    val serviceUuid: UUID,
    val scenarioUuid: UUID,
    val serviceName: String
)
