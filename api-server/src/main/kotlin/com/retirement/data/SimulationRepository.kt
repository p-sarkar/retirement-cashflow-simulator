package com.retirement.data

import com.retirement.data.DatabaseFactory.dbQuery
import com.retirement.model.ApiMetadata
import com.retirement.model.SavedSimulation
import com.retirement.model.SavedSimulationSummary
import com.retirement.model.SimulationConfig
import com.retirement.model.SimulationResult
import com.retirement.util.ServerInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
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

    /** All saved simulations, most recently changed (updated, else created) first (FR005-011). */
    suspend fun list(): List<SavedSimulationSummary> = dbQuery {
        Simulations.selectAll()
            .orderBy(Coalesce(Simulations.updatedAt, Simulations.createdAt) to SortOrder.DESC)
            .map { toSaved(it).summary }
    }

    /** Replace the inputs and results, keeping the name and creation date (FR005-007). Null if [id] doesn't exist. */
    suspend fun update(id: UUID, result: SimulationResult): SavedSimulationSummary? = dbQuery {
        val updated = Simulations.update({ Simulations.id eq id }) {
            it[updatedAt] = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            it[configuration] = json.encodeToString(SimulationConfig.serializer(), result.config)
            it[Simulations.result] = json.encodeToString(SimulationResult.serializer(), result)
        }
        if (updated == 0) null else find(id)?.summary
    }

    /** Change only the name (FR005-008). Null if [id] doesn't exist. */
    suspend fun rename(id: UUID, name: String): SavedSimulationSummary? = dbQuery {
        val updated = Simulations.update({ Simulations.id eq id }) { it[Simulations.name] = name }
        if (updated == 0) null else find(id)?.summary
    }

    /** Delete permanently (FR005-009). False if [id] doesn't exist. */
    suspend fun delete(id: UUID): Boolean = dbQuery {
        Simulations.deleteWhere { Simulations.id eq id } > 0
    }

    suspend fun get(id: UUID): SavedSimulation? = dbQuery { find(id) }

    private fun find(id: UUID): SavedSimulation? =
        Simulations.selectAll().where { Simulations.id eq id }.singleOrNull()?.let(::toSaved)

    private fun toSaved(row: ResultRow): SavedSimulation {
        // Read the version from the stored JSON itself, so results saved without one still load (as an unknown version)
        val stored = json.parseToJsonElement(row[Simulations.result]).jsonObject
        val version = stored["apiMetadata"]?.jsonObject?.get("version")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val decodable = if ("apiMetadata" in stored) stored else JsonObject(stored + ("apiMetadata" to UNKNOWN_METADATA))
        val result = json.decodeFromJsonElement(SimulationResult.serializer(), decodable)
        return SavedSimulation(
            summary = summary(row[Simulations.id].value, row[Simulations.name], row[Simulations.createdAt], result, version)
                .copy(updatedAt = row[Simulations.updatedAt]?.let(::offsetDate)),
            result = result
        )
    }

    private fun offsetDate(date: LocalDateTime) = date.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()

    private val UNKNOWN_METADATA = json.encodeToJsonElement(ApiMetadata.serializer(), ApiMetadata(version = "", buildTime = "", serverStartTime = ""))

    private fun summary(
        id: UUID, name: String, createdAt: LocalDateTime, result: SimulationResult,
        version: String? = result.apiMetadata.version.takeIf { it.isNotBlank() }
    ) = SavedSimulationSummary(
        id = id.toString(),
        name = name,
        createdAt = offsetDate(createdAt),
        updatedAt = null,
        isSuccess = result.summary.isSuccess,
        failureYear = result.summary.failureYear,
        finalTotalBalance = result.summary.finalTotalBalance,
        endAge = result.config.endAge,
        version = version,
        isCurrentVersion = version == ServerInfo.version
    )
}
