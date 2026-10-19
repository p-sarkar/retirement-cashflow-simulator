package com.retirement.api

import com.retirement.logic.BreakdownGenerator
import com.retirement.logic.ConfigValidator
import com.retirement.logic.InputsFiles
import com.retirement.logic.InputsImport
import com.retirement.logic.SimulationEngine
import com.retirement.logic.SimulationTrace
import com.retirement.logic.SpendingStrategyCatalog
import com.retirement.logic.ValidationError
import com.retirement.data.HistoricalDataService
import com.retirement.data.SimulationRepository
import com.retirement.model.MarketPresets
import com.retirement.model.RenameSimulationRequest
import com.retirement.model.SaveSimulationRequest
import com.retirement.model.SimulationConfig
import com.retirement.model.UpdateSimulationRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BreakdownRequest(
    val config: SimulationConfig,
    val targetAge: Int
)

/**
 * 422 response body for invalid simulation input (see contracts/api-contract.md).
 */
@Serializable
data class ValidationErrorResponse(
    val error: String,
    val statusCode: Int,
    val validationErrors: List<ValidationError>
)

/** JSON body for other errors (400, 404). */
@Serializable
data class ErrorResponse(
    val error: String,
    val statusCode: Int
)

/**
 * Validates the config (plus any request-specific [extraErrors]) and responds with 422 if invalid.
 * Returns true when a response was sent and the handler should stop.
 */
internal suspend fun ApplicationCall.rejectIfInvalid(
    config: SimulationConfig,
    extraErrors: List<ValidationError> = emptyList()
): Boolean {
    val errors = ConfigValidator.validate(config).errors + extraErrors
    if (errors.isEmpty()) return false

    respond(
        HttpStatusCode.UnprocessableEntity,
        ValidationErrorResponse(
            error = "Validation failed",
            statusCode = HttpStatusCode.UnprocessableEntity.value,
            validationErrors = errors
        )
    )
    return true
}

internal suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) =
    respond(status, ErrorResponse(message, status.value))

/**
 * An interactive run: the config's static rates, except the equity returns of a market preset's
 * return sequence (e.g. the 2008 Crash) when one is selected. Guardrail reviews draw their market paths from
 * the historical data (spec 006, R006-3), loaded once per request.
 */
private fun runInteractive(config: SimulationConfig, trace: SimulationTrace? = null) = SimulationEngine.runSimulation(
    config,
    marketReturns = MarketPresets.equityReturns(config),
    inflationRates = emptyList(),
    trace = trace,
    history = HistoricalDataService.getHistoricalYears()
)

fun Route.simulationRoutes() {
    route("/api") {
        post("/simulate") {
            val config = call.receive<SimulationConfig>()
            if (call.rejectIfInvalid(config)) return@post

            call.respond(runInteractive(config))
        }

        post("/simulate/breakdown") {
            val request = call.receive<BreakdownRequest>()
            val config = request.config
            val targetAge = request.targetAge
            if (call.rejectIfInvalid(config)) return@post

            // A traced run of the same interactive simulation, so the breakdown can follow every movement
            val trace = SimulationTrace()
            val result = runInteractive(config, trace)
            if (result.yearlyResults.none { it.age == targetAge }) {
                call.respondError(HttpStatusCode.BadRequest, "Age $targetAge is not in the simulation")
                return@post
            }
            val breakdown = BreakdownGenerator.generate(config, result, trace, targetAge)

            call.respond(breakdown)
        }

        get("/market-presets") {
            call.respond(MarketPresets.all)
        }

        // The spending strategies the form can offer (spec 006, FR006-002)
        get("/spending-strategies") {
            call.respond(SpendingStrategyCatalog.all)
        }
    }
}

/** Saved simulations (US002-2; update, rename, delete US005-2). Needs DatabaseFactory.init(). */
fun Route.savedSimulationRoutes() {
    route("/api/simulations") {
        get {
            call.respond(SimulationRepository.list())
        }

        post {
            val request = call.receive<SaveSimulationRequest>()
            val name = call.validName(request.name) ?: return@post
            call.respond(HttpStatusCode.Created, SimulationRepository.save(name, request.result))
        }

        get("/{id}") {
            val id = call.simulationId() ?: return@get
            val saved = SimulationRepository.get(id) ?: return@get call.respondNotFound(id)
            call.respond(saved)
        }

        put("/{id}") {
            val id = call.simulationId() ?: return@put
            val request = call.receive<UpdateSimulationRequest>()
            val updated = SimulationRepository.update(id, request.result) ?: return@put call.respondNotFound(id)
            call.respond(updated)
        }

        patch("/{id}") {
            val id = call.simulationId() ?: return@patch
            val name = call.validName(call.receive<RenameSimulationRequest>().name) ?: return@patch
            val renamed = SimulationRepository.rename(id, name) ?: return@patch call.respondNotFound(id)
            call.respond(renamed)
        }

        delete("/{id}") {
            val id = call.simulationId() ?: return@delete
            if (SimulationRepository.delete(id)) call.respond(HttpStatusCode.NoContent) else call.respondNotFound(id)
        }
    }
}

/** Export and import a simulation's inputs as a file (US005-3, R005-9). */
fun Route.inputsRoutes() {
    route("/api/inputs") {
        post("/export") {
            val config = call.receive<SimulationConfig>()
            if (call.rejectIfInvalid(config)) return@post
            call.respond(InputsFiles.export(config))
        }

        // The body is the file's text, read here so that anything that isn't an inputs file is a 400, not a 500
        post("/import") {
            when (val read = InputsFiles.read(call.receiveText())) {
                is InputsImport.Valid -> call.respond(read.config)
                is InputsImport.Invalid -> call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ValidationErrorResponse("Validation failed", HttpStatusCode.UnprocessableEntity.value, read.errors)
                )
                is InputsImport.NotAnInputsFile -> call.respondError(HttpStatusCode.BadRequest, read.reason)
            }
        }
    }
}

/** The `{id}` path parameter, or null after responding 400 when it isn't a UUID. */
private suspend fun ApplicationCall.simulationId(): UUID? {
    val id = parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (id == null) respondError(HttpStatusCode.BadRequest, "Simulation id must be a UUID")
    return id
}

private suspend fun ApplicationCall.respondNotFound(id: UUID) =
    respondError(HttpStatusCode.NotFound, "Saved simulation $id not found")

/** The trimmed name, or null after responding 422 when it is blank or too long (FR002-011, FR005-008). */
private suspend fun ApplicationCall.validName(raw: String): String? {
    val name = raw.trim()
    val nameError = when {
        name.isEmpty() -> "Name is required"
        name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or less"
        else -> null
    }
    if (nameError == null) return name
    respond(
        HttpStatusCode.UnprocessableEntity,
        ValidationErrorResponse("Validation failed", 422, listOf(ValidationError("name", nameError)))
    )
    return null
}

private const val MAX_NAME_LENGTH = 255 // Simulations.name column size
