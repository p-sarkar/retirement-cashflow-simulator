package com.retirement.api

import com.retirement.logic.BreakdownGenerator
import com.retirement.logic.ConfigValidator
import com.retirement.logic.SimulationEngine
import com.retirement.logic.ValidationError
import com.retirement.model.SimulationConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

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

/**
 * Validates the config and responds with 422 if invalid.
 * Returns true when a response was sent and the handler should stop.
 */
private suspend fun ApplicationCall.rejectIfInvalid(config: SimulationConfig): Boolean {
    val validation = ConfigValidator.validate(config)
    if (validation.isValid) return false

    respond(
        HttpStatusCode.UnprocessableEntity,
        ValidationErrorResponse(
            error = "Validation failed",
            statusCode = HttpStatusCode.UnprocessableEntity.value,
            validationErrors = validation.errors
        )
    )
    return true
}

fun Route.simulationRoutes() {
    route("/api") {
        post("/simulate") {
            val config = call.receive<SimulationConfig>()
            if (call.rejectIfInvalid(config)) return@post

            // For US1 single run, we generally use the static assumptions provided in the config.
            // We pass empty lists to force SimulationEngine to use config.rates.
            val result = SimulationEngine.runSimulation(
                config, 
                marketReturns = emptyList(), 
                inflationRates = emptyList()
            )
            call.respond(result)
        }

        post("/simulate/breakdown") {
            val request = call.receive<BreakdownRequest>()
            val config = request.config
            val targetAge = request.targetAge
            if (call.rejectIfInvalid(config)) return@post

            // Run simulation to get results
            val result = SimulationEngine.runSimulation(
                config,
                marketReturns = emptyList(),
                inflationRates = emptyList()
            )

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
    }
}
