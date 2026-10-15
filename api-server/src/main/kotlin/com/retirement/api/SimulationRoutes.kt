package com.retirement.api

import com.retirement.logic.BreakdownGenerator
import com.retirement.logic.ConfigValidator
import com.retirement.logic.SimulationEngine
import com.retirement.logic.ValidationError
import com.retirement.data.SimulationRepository
import com.retirement.model.MarketPresets
import com.retirement.model.SaveSimulationRequest
import com.retirement.model.SimulationConfig
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
 * return sequence (e.g. the 2008 Crash) when one is selected.
 */
private fun runInteractive(config: SimulationConfig) = SimulationEngine.runSimulation(
    config,
    marketReturns = MarketPresets.equityReturns(config),
    inflationRates = emptyList()
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

            // Run simulation to get results
            val result = runInteractive(config)

            // Find the yearly result for the target age
            val yearlyResult = result.yearlyResults.find { it.age == targetAge }
            if (yearlyResult == null) {
                call.respond(HttpStatusCode.BadRequest, "Age $targetAge not found in simulation results")
                return@post
            }

            // Find prior year result
            val priorYearResult = result.yearlyResults.find { it.age == targetAge - 1 }

            // Generate breakdown
            val breakdown = BreakdownGenerator.generateBreakdown(
                config,
                targetAge,
                yearlyResult,
                priorYearResult
            )

            call.respond(breakdown)
        }

        get("/market-presets") {
            call.respond(MarketPresets.all)
        }
    }
}

/** Saved simulations (spec 002 US2). Needs DatabaseFactory.init(). */
fun Route.savedSimulationRoutes() {
    route("/api/simulations") {
        get {
            call.respond(SimulationRepository.list())
        }

        post {
            val request = call.receive<SaveSimulationRequest>()
            val name = request.name.trim()
            val nameError = when {
                name.isEmpty() -> "Name is required"
                name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or less"
                else -> null
            }
            if (nameError != null) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ValidationErrorResponse("Validation failed", 422, listOf(ValidationError("name", nameError)))
                )
                return@post
            }
            call.respond(HttpStatusCode.Created, SimulationRepository.save(name, request.result))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (id == null) {
                call.respondError(HttpStatusCode.BadRequest, "Simulation id must be a UUID")
                return@get
            }
            val saved = SimulationRepository.get(id)
            if (saved == null) {
                call.respondError(HttpStatusCode.NotFound, "Saved simulation $id not found")
                return@get
            }
            call.respond(saved)
        }
    }
}

private const val MAX_NAME_LENGTH = 255 // Simulations.name column size
