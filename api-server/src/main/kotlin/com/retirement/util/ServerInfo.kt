package com.retirement.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ServerInfo {
    val version = "1.0.0"
    val buildTime: String = Instant.now().atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val serverStartTime: String = Instant.now().atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    fun getMetadata() = com.retirement.model.ApiMetadata(
        version = version,
        buildTime = buildTime,
        serverStartTime = serverStartTime
    )
}

