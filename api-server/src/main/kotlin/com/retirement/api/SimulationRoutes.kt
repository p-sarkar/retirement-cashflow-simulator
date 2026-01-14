package com.retirement.api

import com.retirement.logic.BreakdownGenerator
import com.retirement.logic.SimulationEngine
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

fun Route.simulationRoutes() {
    route("/api") {
        post("/simulate") {
            val config = call.receive<SimulationConfig>()
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
