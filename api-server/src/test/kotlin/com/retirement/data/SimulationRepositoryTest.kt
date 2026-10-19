package com.retirement.data

import com.retirement.logic.SimulationEngine
import com.retirement.testConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulationRepositoryTest {

    private fun tempUrl() = "jdbc:sqlite:" + File.createTempFile("simulations", ".db").apply { deleteOnExit() }.absolutePath

    @BeforeTest
    fun freshDatabase() {
        DatabaseFactory.init(tempUrl())
    }

    private val result = SimulationEngine.runSimulation(testConfig(), emptyList(), emptyList())

    @Test
    fun `a saved simulation reloads with its inputs and results intact (SC002-004)`() = runBlocking {
        val saved = SimulationRepository.save("Baseline", result)
        val loaded = assertNotNull(SimulationRepository.get(UUID.fromString(saved.id)))

        assertEquals(result, loaded.result)
        assertEquals(saved, loaded.summary)
    }

    @Test
    fun `the summary has the name, date and outcome`() = runBlocking {
        val saved = SimulationRepository.save("Baseline", result)

        assertEquals("Baseline", saved.name)
        assertEquals(result.summary.isSuccess, saved.isSuccess)
        assertEquals(result.summary.finalTotalBalance, saved.finalTotalBalance)
        assertEquals(85, saved.endAge)
        OffsetDateTime.parse(saved.createdAt) // ISO-8601 with offset
        Unit
    }

    @Test
    fun `the list is newest first`() = runBlocking {
        val first = SimulationRepository.save("First", result)
        Thread.sleep(1100) // createdAt has 1-second resolution
        val second = SimulationRepository.save("Second", result)

        assertEquals(listOf(second.id, first.id), SimulationRepository.list().map { it.id })
    }

    @Test
    fun `an unknown id is not found`() = runBlocking {
        assertNull(SimulationRepository.get(UUID.randomUUID()))
    }

    // --- T005-002: the updated_at column is added to databases saved before spec 005 (R005-6) ---

    /** The Simulations table as it was before spec 005, without updated_at. */
    private object OldSimulations : UUIDTable("Simulations") {
        val name = varchar("name", 255)
        val createdAt = datetime("created_at")
        val configuration = text("configuration")
        val result = text("result")
    }

    private fun columns(url: String) = transaction(Database.connect(url, "org.sqlite.JDBC")) {
        exec("PRAGMA table_info(Simulations)") { rs -> generateSequence { if (rs.next()) rs.getString("name") else null }.toList() }!!
    }

    @Test
    fun `an old database gains updated_at and keeps its saved simulations`() = runBlocking {
        val url = tempUrl()
        val createdAt = LocalDateTime.of(2026, 10, 1, 9, 30)
        val json = kotlinx.serialization.json.Json
        val configJson = json.encodeToString(com.retirement.model.SimulationConfig.serializer(), result.config)
        val resultJson = json.encodeToString(com.retirement.model.SimulationResult.serializer(), result)
        transaction(Database.connect(url, "org.sqlite.JDBC")) {
            SchemaUtils.create(OldSimulations)
            OldSimulations.insert {
                it[name] = "Saved before spec 005"
                it[OldSimulations.createdAt] = createdAt
                it[configuration] = configJson
                it[OldSimulations.result] = resultJson
            }
        }
        assertTrue("updated_at" !in columns(url))

        DatabaseFactory.init(url)
        assertTrue("updated_at" in columns(url))
        val saved = SimulationRepository.list().single()
        assertEquals("Saved before spec 005", saved.name)
        assertEquals(result, SimulationRepository.get(UUID.fromString(saved.id))!!.result)
        transaction { assertNull(Simulations.selectAll().single()[Simulations.updatedAt]) }

        // A second start changes nothing
        DatabaseFactory.init(url)
        assertEquals(columns(url).size, columns(url).distinct().size)
        assertEquals(listOf(saved), SimulationRepository.list())
    }

    // --- T005-004: which version computed the saved results (FR005-001, R005-1) ---

    @Test
    fun `results saved by the running version are current`() = runBlocking {
        val saved = SimulationRepository.save("Current", result)

        assertEquals(com.retirement.util.ServerInfo.version, saved.version)
        assertTrue(saved.isCurrentVersion)
    }

    @Test
    fun `results saved by another version are flagged with that version`() = runBlocking {
        val older = result.copy(apiMetadata = result.apiMetadata.copy(version = "1.1.28"))
        val saved = SimulationRepository.save("Older", older)

        assertEquals("1.1.28", saved.version)
        assertEquals(false, saved.isCurrentVersion)
        assertEquals(saved, SimulationRepository.list().single())
    }

    @Test
    fun `results without a recorded version list as an unknown version`() = runBlocking {
        val json = kotlinx.serialization.json.Json
        val resultJson = json.encodeToJsonElement(com.retirement.model.SimulationResult.serializer(), result).jsonObject
        val withoutMetadata = kotlinx.serialization.json.JsonObject(resultJson - "apiMetadata").toString()
        val configJson = json.encodeToString(com.retirement.model.SimulationConfig.serializer(), result.config)
        transaction {
            Simulations.insert {
                it[name] = "No version"
                it[createdAt] = LocalDateTime.now()
                it[configuration] = configJson
                it[Simulations.result] = withoutMetadata
            }
        }

        val listed = SimulationRepository.list().single()
        assertNull(listed.version)
        assertEquals(false, listed.isCurrentVersion)
        assertEquals(result.yearlyResults, SimulationRepository.get(UUID.fromString(listed.id))!!.result.yearlyResults)
    }

    // --- T005-013: update, rename, delete, order by last change (FR005-007 to FR005-011) ---

    private val edited = SimulationEngine.runSimulation(testConfig(retirementAge = 62), emptyList(), emptyList())

    @Test
    fun `an update replaces inputs and results, keeps name and creation date, and records when`() = runBlocking {
        val older = result.copy(apiMetadata = result.apiMetadata.copy(version = "1.1.28"))
        val saved = SimulationRepository.save("Plan", older)
        assertNull(saved.updatedAt)

        val updated = assertNotNull(SimulationRepository.update(UUID.fromString(saved.id), edited))
        assertEquals("Plan", updated.name)
        assertEquals(saved.createdAt, updated.createdAt)
        OffsetDateTime.parse(updated.updatedAt!!)
        assertEquals(com.retirement.util.ServerInfo.version, updated.version)
        assertTrue(updated.isCurrentVersion)
        assertEquals(edited, SimulationRepository.get(UUID.fromString(saved.id))!!.result)
    }

    @Test
    fun `a rename changes only the name`() = runBlocking {
        val saved = SimulationRepository.save("Plan", result)
        val renamed = assertNotNull(SimulationRepository.rename(UUID.fromString(saved.id), "Retire at 65"))

        assertEquals(saved.copy(name = "Retire at 65"), renamed)
        assertEquals(result, SimulationRepository.get(UUID.fromString(saved.id))!!.result)
    }

    @Test
    fun `a delete removes the simulation`() = runBlocking {
        val kept = SimulationRepository.save("Kept", result)
        val gone = SimulationRepository.save("Gone", result)

        assertTrue(SimulationRepository.delete(UUID.fromString(gone.id)))
        assertEquals(listOf(kept.id), SimulationRepository.list().map { it.id })
        assertEquals(false, SimulationRepository.delete(UUID.fromString(gone.id)))
    }

    @Test
    fun `update, rename and delete of an unknown id find nothing`() = runBlocking {
        val unknown = UUID.randomUUID()
        assertNull(SimulationRepository.update(unknown, result))
        assertNull(SimulationRepository.rename(unknown, "Name"))
        assertEquals(false, SimulationRepository.delete(unknown))
    }

    @Test
    fun `the list is ordered by the last change`() = runBlocking {
        val first = SimulationRepository.save("First", result)
        Thread.sleep(1100) // dates have 1-second resolution
        val second = SimulationRepository.save("Second", result)
        Thread.sleep(1100)
        SimulationRepository.update(UUID.fromString(first.id), edited)

        assertEquals(listOf(first.id, second.id), SimulationRepository.list().map { it.id })
    }
}
