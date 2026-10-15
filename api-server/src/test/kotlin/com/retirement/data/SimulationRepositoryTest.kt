package com.retirement.data

import com.retirement.logic.SimulationEngine
import com.retirement.testConfig
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SimulationRepositoryTest {

    @BeforeTest
    fun freshDatabase() {
        val file = File.createTempFile("simulations", ".db").apply { deleteOnExit() }
        DatabaseFactory.init("jdbc:sqlite:${file.absolutePath}")
    }

    private val result = SimulationEngine.runSimulation(testConfig(), emptyList(), emptyList())

    @Test
    fun `a saved simulation reloads with its inputs and results intact (SC-004)`() = runBlocking {
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
}
