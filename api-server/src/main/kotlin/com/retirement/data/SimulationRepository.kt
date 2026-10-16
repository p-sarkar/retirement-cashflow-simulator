package com.retirement.data

import com.retirement.data.DatabaseFactory.dbQuery
import com.retirement.model.SavedSimulation
import com.retirement.model.SavedSimulationSummary
import com.retirement.model.SimulationConfig
import com.retirement.model.SimulationResult
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Saved simulations (US002-2): the config and the full result, stored as JSON so a reload
 * shows exactly what was saved (SC002-004), even after the engine changes.
 */
object SimulationRepository {
    // Saved JSON may come from an older model: ignore removed fields, use defaults for added ones
    private val json = Json { ignoreUnknownKeys = true }

    /** Save a simulation under [name] (the result's config keeps the name it was run with). */
    suspend fun save(name: String, result: SimulationResult): SavedSimulationSummary = dbQuery {
        val createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val id = Simulations.insertAndGetId {
            it[Simulations.name] = name
            it[Simulations.createdAt] = createdAt
            it[configuration] = json.encodeToString(SimulationConfig.serializer(), result.config)
            it[Simulations.result] = json.encodeToString(SimulationResult.serializer(), result)
        }
        summary(id.value, name, createdAt, result)
    }

    /** All saved simulations, newest first. */
    suspend fun list(): List<SavedSimulationSummary> = dbQuery {
        Simulations.selectAll()
            .orderBy(Simulations.createdAt to SortOrder.DESC)
            .map { toSaved(it).summary }
    }

    suspend fun get(id: UUID): SavedSimulation? = dbQuery {
        Simulations.selectAll().where { Simulations.id eq id }.singleOrNull()?.let(::toSaved)
    }

    private fun toSaved(row: ResultRow): SavedSimulation {
        val result = json.decodeFromString(SimulationResult.serializer(), row[Simulations.result])
        return SavedSimulation(
            summary = summary(row[Simulations.id].value, row[Simulations.name], row[Simulations.createdAt], result),
            result = result
        )
    }

    private fun summary(id: UUID, name: String, createdAt: LocalDateTime, result: SimulationResult) = SavedSimulationSummary(
        id = id.toString(),
        name = name,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString(),
        isSuccess = result.summary.isSuccess,
        failureYear = result.summary.failureYear,
        finalTotalBalance = result.summary.finalTotalBalance,
        endAge = result.config.endAge
    )
}
