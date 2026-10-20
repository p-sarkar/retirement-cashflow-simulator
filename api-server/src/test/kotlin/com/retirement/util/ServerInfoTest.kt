package com.retirement.util

import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The version the server reports is the released version in api-server/version.properties (major.minor.patch). */
class ServerInfoTest {

    @Test
    fun `the reported version is the released version`() {
        val released = Properties().apply { File("version.properties").inputStream().use { load(it) } }
        val expected = listOf("majorVersion", "minorVersion", "patchVersion").joinToString(".") { released.getProperty(it) }

        assertEquals(expected, ServerInfo.version)
        assertTrue(ServerInfo.buildTime.isNotBlank())
    }

    // The build: the version plus SemVer build metadata from `git describe`, which never changes the version itself

    @Test
    fun `a build on the release tag is the version alone`() {
        assertEquals("1.1.29", ServerInfo.buildId("1.1.29", "v1.1.29"))
    }

    @Test
    fun `a build after the release tag adds the commits since it and the commit`() {
        assertEquals("1.1.29+16.gbff4a24", ServerInfo.buildId("1.1.29", "v1.1.29-16-gbff4a24"))
        assertEquals("1.1.29+16.gbff4a24.dirty", ServerInfo.buildId("1.1.29", "v1.1.29-16-gbff4a24-dirty"))
        assertEquals("1.1.29+dirty", ServerInfo.buildId("1.1.29", "v1.1.29-dirty"))
    }

    @Test
    fun `a version not tagged yet is identified by its commit`() {
        // e.g. after ./gradlew release, before the release is merged and tagged: git describe gives only the commit
        assertEquals("1.1.30+gbff4a24", ServerInfo.buildId("1.1.30", "bff4a24"))
        assertEquals("1.1.30+gbff4a24.dirty", ServerInfo.buildId("1.1.30", "bff4a24-dirty"))
    }

    @Test
    fun `a build outside a git checkout is marked unknown`() {
        assertEquals("1.1.29+unknown", ServerInfo.buildId("1.1.29", null))
        assertEquals("1.1.29+unknown", ServerInfo.buildId("1.1.29", ""))
    }

    @Test
    fun `the server reports its build next to the unchanged version`() {
        val metadata = ServerInfo.getMetadata()
        assertEquals(ServerInfo.version, metadata.version)
        assertTrue(metadata.build == metadata.version || metadata.build.startsWith(metadata.version + "+"), metadata.build)
    }
}
