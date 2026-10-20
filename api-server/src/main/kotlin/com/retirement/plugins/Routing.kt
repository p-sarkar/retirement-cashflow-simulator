package com.retirement.plugins

import com.retirement.api.inputsRoutes
import com.retirement.api.monteCarloRoutes
import com.retirement.api.savedSimulationRoutes
import com.retirement.api.simulationRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        simulationRoutes()
        monteCarloRoutes()
        savedSimulationRoutes()
        inputsRoutes()
    }
}