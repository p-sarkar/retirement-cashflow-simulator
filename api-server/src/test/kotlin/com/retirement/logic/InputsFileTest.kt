package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.testConfig
import com.retirement.util.ServerInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** T005-027: the exported inputs file and its import (US005-3, FR005-013 to FR005-018, SC005-004). */
class InputsFileTest {

    // As the server writes responses (Ktor's default Json: every field, defaults included)
    private val json = Json { encodeDefaults = true }

    private fun fileText(config: SimulationConfig) = json.encodeToString(InputsFile.serializer(), InputsFiles.export(config))

    private fun run(config: SimulationConfig) =
        SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList(), logExpenses = false)

    @Test
    fun `an export holds the inputs, the format and the version that wrote it (FR005-013)`() {
        val file = InputsFiles.export(defaultFormConfig())

        assertEquals("retirement-simulator-inputs", file.format)
        assertEquals(1, file.formatVersion)
        assertEquals(ServerInfo.version, file.appVersion)
        assertEquals(defaultFormConfig(), file.config)
    }

    @Test
    fun `exporting and importing gives the same inputs and the same results (FR005-018, SC005-004)`() {
        (listOf(defaultFormConfig(), testConfig()) + presetConfigs()).forEach { config ->
            val imported = assertIs<InputsImport.Valid>(InputsFiles.read(fileText(config)), config.name).config

            assertEquals(config, imported, config.name)
            assertEquals(run(config).yearlyResults, run(imported).yearlyResults, config.name)
        }
    }

    @Test
    fun `inputs missing from an older file take their defaults (FR005-017)`() {
        val file = json.parseToJsonElement(fileText(testConfig())).jsonObject
        val config = file.getValue("config").jsonObject
        val older = JsonObject(file + ("config" to JsonObject(config - "endAge")))

        val imported = assertIs<InputsImport.Valid>(InputsFiles.read(older.toString())).config
        assertEquals(85, imported.endAge)
    }

    @Test
    fun `a file that isn't an inputs file is refused (FR005-016)`() {
        val file = json.parseToJsonElement(fileText(testConfig())).jsonObject
        val wrongFormat = JsonObject(file + ("format" to JsonPrimitive("something-else"))).toString()
        val noFormat = JsonObject(file - "format").toString()
        val newer = JsonObject(file + ("formatVersion" to JsonPrimitive(2))).toString()
        val noConfig = JsonObject(file - "config").toString()

        listOf(wrongFormat, noFormat, newer, noConfig, "not json", "Year,Age\n2026,51", "[]").forEach { text ->
            assertIs<InputsImport.NotAnInputsFile>(InputsFiles.read(text), text.take(40))
        }
    }

    @Test
    fun `invalid inputs are listed all at once, with the same fields as a run (FR005-015)`() {
        val invalid = testConfig().copy(retirementAge = 5, currentYear = 1800)
        val result = assertIs<InputsImport.Invalid>(InputsFiles.read(fileText(invalid)))

        assertEquals(ConfigValidator.validate(invalid).errors, result.errors)
        assertTrue(result.errors.map { it.field }.containsAll(listOf("retirementAge", "currentYear")))
    }
}
