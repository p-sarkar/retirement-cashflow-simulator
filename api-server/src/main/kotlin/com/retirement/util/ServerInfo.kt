package com.retirement.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties

object ServerInfo {
    private val versionProps: Properties by lazy {
        Properties().apply {
            try {
                val inputStream = ServerInfo::class.java.getResourceAsStream("/version.properties")
                if (inputStream != null) {
                    load(inputStream)
                    inputStream.close()
                }
            } catch (e: Exception) {
                // Fallback values if file not found
            }
        }
    }

    val version: String = versionProps.getProperty("version", "1.0.0")
    val buildTime: String = versionProps.getProperty("buildTime",
        Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
    val serverStartTime: String = Instant.now().atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    fun getMetadata() = com.retirement.model.ApiMetadata(
        version = version,
        buildTime = buildTime,
        serverStartTime = serverStartTime
    )
}

