package com.retirement.api

import com.retirement.logic.MonteCarloEngine
import com.retirement.logic.ValidationError
import com.retirement.model.MonteCarloRequest
import com.retirement.model.MonteCarloRunRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.random.Random

/** Monte Carlo analysis (US002-3) and single-run details (US002-4). */
fun Route.monteCarloRoutes() {
    route("/api/simulate/monte-carlo") {
        post {
            val request = call.receive<MonteCarloRequest>()
            val runsError = if (request.runs !in 1..MonteCarloEngine.MAX_RUNS) {
                listOf(ValidationError("runs", "Runs must be from 1 to ${MonteCarloEngine.MAX_RUNS}"))
            } else emptyList()
            if (call.rejectIfInvalid(request.config, runsError)) return@post

            // Without a seed, pick one; the result returns it so the analysis (and each run) can be reproduced.
            // Kept within JavaScript's safe integer range so the browser can send it back unchanged.
            val seed = request.seed ?: Random.nextLong(MAX_SAFE_SEED)
            call.respond(MonteCarloEngine.run(request.config, request.runs, seed))
        }

        // Full details of one run, rebuilt from the analysis seed (the analysis returns only summaries)
        post("/run") {
            val request = call.receive<MonteCarloRunRequest>()
            val indexError = if (request.runIndex < 0) {
                listOf(ValidationError("runIndex", "Run index cannot be negative"))
            } else emptyList()
            if (call.rejectIfInvalid(request.config, indexError)) return@post

            call.respond(MonteCarloEngine.runDetail(request.config, request.seed, request.runIndex))
        }
    }
}

/** Number.MAX_SAFE_INTEGER: larger seeds lose precision as JSON numbers in the browser. */
private const val MAX_SAFE_SEED = 9_007_199_254_740_991L
