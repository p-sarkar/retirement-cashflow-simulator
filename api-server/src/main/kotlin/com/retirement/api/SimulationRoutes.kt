package com.retirement.api

import com.retirement.logic.SimulationEngine
import com.retirement.model.SimulationConfig
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
    }
}
