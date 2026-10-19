package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.MarketPresets
import com.retirement.model.SimulationConfig
import com.retirement.model.SimulationResult
import com.retirement.model.MonteCarloResult
import com.retirement.presetConfigs
import com.retirement.testConfig
import io.ktor.serialization.kotlinx.json.DefaultJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.fail

/**
 * Results of "Partha's Spending Strategy-v0.01-20260105" must never change (FR006-007, SC006-001, research R006-9).
 * Snapshots were taken from the code before spec 006; regenerate them only for a deliberate change to the
 * existing strategy, with GOLDEN_UPDATE=1.
 *
 * Results are serialized as the server responds (Ktor's DefaultJson writes out defaults). Every snapshot field
 * must be present and equal; a field the snapshot lacks is allowed only if it is null or a new
 * default-valued setting (analysis I006-1).
 */
class ExistingStrategyGoldenTest {

    private val dir = File("src/test/resources/golden/006")
    private val update = System.getenv("GOLDEN_UPDATE") == "1"

    /** New fields that appear with their default value in older configs' echo. */
    private val defaultValuedPaths = listOf(".strategy.guardrails")

    private fun interactive(config: SimulationConfig): SimulationResult =
        SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList())

    private fun cases(): Map<String, () -> JsonElement> {
        val configs = listOf("test-config" to testConfig(), "default-form" to defaultFormConfig()) +
            presetConfigs().map { "preset-${it.marketPresetId}" to it }
        val simulations = configs.associate { (name, config) ->
            name to { DefaultJson.encodeToJsonElement(SimulationResult.serializer(), interactive(config)) }
        }
        val monteCarlo = "monte-carlo-50-seed-42" to {
            val result = runBlocking { MonteCarloEngine.run(defaultFormConfig(), runs = 50, seed = 42L) }
            DefaultJson.encodeToJsonElement(MonteCarloResult.serializer(), result)
        }
        return simulations + monteCarlo
    }

    @Test
    fun `existing strategy results match the snapshots taken before spec 006`() {
        val failures = mutableListOf<String>()
        for ((name, produce) in cases()) {
            val actual = withoutMetadata(produce())
            val file = File(dir, "$name.json")
            if (update) {
                dir.mkdirs()
                file.writeText(Json.encodeToString(JsonElement.serializer(), actual))
                continue
            }
            if (!file.exists()) {
                failures += "$name: snapshot ${file.path} is missing (run with GOLDEN_UPDATE=1 on unchanged code)"
                continue
            }
            val expected = Json.parseToJsonElement(file.readText())
            compare(expected, actual, name, failures)
        }
        if (failures.isNotEmpty()) fail(failures.take(20).joinToString("\n") +
            if (failures.size > 20) "\n… ${failures.size - 20} more" else "")
    }

    private fun withoutMetadata(element: JsonElement): JsonElement = JsonObject(element.jsonObject - "apiMetadata")

    private fun compare(expected: JsonElement, actual: JsonElement, path: String, failures: MutableList<String>) {
        when (expected) {
            is JsonObject -> {
                val a = actual as? JsonObject ?: return run { failures += "$path: expected an object" }
                for ((key, value) in expected) {
                    val other = a[key] ?: run { failures += "$path.$key: missing"; null } ?: continue
                    compare(value, other, "$path.$key", failures)
                }
                for ((key, value) in a) {
                    if (key in expected) continue
                    val allowed = value is JsonNull || defaultValuedPaths.any { "$path.$key".endsWith(it) }
                    if (!allowed) failures += "$path.$key: new field with a non-null value $value"
                }
            }
            is JsonArray -> {
                val a = actual as? JsonArray ?: return run { failures += "$path: expected an array" }
                if (a.size != expected.size) failures += "$path: size ${a.size}, expected ${expected.size}"
                expected.zip(a).forEachIndexed { i, (e, v) -> compare(e, v, "$path[$i]", failures) }
            }
            is JsonPrimitive -> {
                val a = actual as? JsonPrimitive ?: return run { failures += "$path: expected a value" }
                val e = expected.doubleOrNull
                val v = a.doubleOrNull
                val equal = if (e != null && v != null && !expected.isString) {
                    abs(e - v) <= 1e-9 * max(1.0, abs(e))
                } else expected == a
                if (!equal) failures += "$path: $a, expected $expected"
            }
        }
    }
}
