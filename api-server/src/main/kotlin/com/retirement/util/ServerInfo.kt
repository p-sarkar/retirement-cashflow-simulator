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
    // Declared before build, which uses it while the object initializes
    private val describedBuild = Regex("^v.+-(\\d+)-g([0-9a-f]+)$")
    val build: String = buildId(version, versionProps.getProperty("gitDescribe"))

    /**
     * The version plus SemVer build metadata from `git describe` (which never changes the version itself):
     * - on the release tag: the version alone (`1.1.29`)
     * - after it: commits since the tag and the commit (`1.1.29+16.gbff4a24`)
     * - before the version is tagged: the commit (`1.1.30+gbff4a24`)
     * - with uncommitted changes: `.dirty`; outside a git checkout: `+unknown`
     */
    fun buildId(version: String, gitDescribe: String?): String {
        val described = gitDescribe?.trim().orEmpty()
        if (described.isEmpty()) return "$version+unknown"
        val dirty = described.endsWith("-dirty")
        val core = described.removeSuffix("-dirty")
        val sinceTag = describedBuild.find(core)
        val metadata = when {
            sinceTag != null -> listOf(sinceTag.groupValues[1], "g" + sinceTag.groupValues[2])
            core.startsWith("v") -> emptyList() // exactly on the release tag
            else -> listOf("g$core") // the commit alone
        } + if (dirty) listOf("dirty") else emptyList()
        return if (metadata.isEmpty()) version else "$version+${metadata.joinToString(".")}"
    }

    fun getMetadata() = com.retirement.model.ApiMetadata(
        version = version,
        buildTime = buildTime,
        serverStartTime = serverStartTime,
        build = build
    )
}

