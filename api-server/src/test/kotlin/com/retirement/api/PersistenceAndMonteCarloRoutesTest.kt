package com.retirement.api

import com.retirement.data.DatabaseFactory
import com.retirement.logic.SimulationEngine
import com.retirement.model.*
import com.retirement.testConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistenceAndMonteCarloRoutesTest {

    @BeforeTest
    fun freshDatabase() {
        val file = File.createTempFile("routes", ".db").apply { deleteOnExit() }
        DatabaseFactory.init("jdbc:sqlite:${file.absolutePath}")
    }

    private fun ApplicationTestBuilder.setup() {
        install(ContentNegotiation) { json() }
        routing {
            simulationRoutes()
            monteCarloRoutes()
            savedSimulationRoutes()
        }
    }

    private suspend fun ApplicationTestBuilder.post(path: String, body: String) =
        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private val result = SimulationEngine.runSimulation(testConfig(), emptyList(), emptyList())

    private fun saveBody(name: String) = Json.encodeToString(SaveSimulationRequest.serializer(), SaveSimulationRequest(name, result))

    private fun errorFields(response: String): List<String> =
        Json.parseToJsonElement(response).jsonObject["validationErrors"]!!.jsonArray
            .map { it.jsonObject["field"]!!.jsonPrimitive.content }

    // --- Saved simulations (US002-2) ---

    @Test
    fun `save, list and load a simulation`() = testApplication {
        setup()
        val created = post("/api/simulations", saveBody("  Retire at 65  "))
        assertEquals(HttpStatusCode.Created, created.status)
        val summary = Json.decodeFromString(SavedSimulationSummary.serializer(), created.bodyAsText())
        assertEquals("Retire at 65", summary.name) // trimmed

        val list = Json.decodeFromString(ListSerializer(SavedSimulationSummary.serializer()), client.get("/api/simulations").bodyAsText())
        assertEquals(listOf(summary), list)

        val loaded = client.get("/api/simulations/${summary.id}")
        assertEquals(HttpStatusCode.OK, loaded.status)
        assertEquals(result, Json.decodeFromString(SavedSimulation.serializer(), loaded.bodyAsText()).result)
    }

    @Test
    fun `a blank name is rejected`() = testApplication {
        setup()
        val response = post("/api/simulations", saveBody("   "))
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(listOf("name"), errorFields(response.bodyAsText()))
    }

    @Test
    fun `loading an unknown or malformed id returns JSON errors`() = testApplication {
        setup()
        val missing = client.get("/api/simulations/00000000-0000-0000-0000-000000000000")
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals(404, Json.parseToJsonElement(missing.bodyAsText()).jsonObject["statusCode"]!!.jsonPrimitive.int)

        assertEquals(HttpStatusCode.BadRequest, client.get("/api/simulations/not-a-uuid").status)
    }

    // --- Market presets (FR002-003) ---

    @Test
    fun `market presets are listed`() = testApplication {
        setup()
        val presets = Json.decodeFromString(ListSerializer(MarketPreset.serializer()), client.get("/api/market-presets").bodyAsText())
        assertEquals(MarketPresets.all, presets)
    }

    @Test
    fun `simulate applies the selected preset's return sequence`() = testApplication {
        setup()
        val portfolio = testConfig().portfolio.copy(tfa = 100000.0)
        fun body(presetId: String) =
            Json.encodeToString(SimulationConfig.serializer(), testConfig(portfolio = portfolio, marketPresetId = presetId))
        fun tfaGrowthIn2031(response: String): Double {
            val years = Json.decodeFromString(SimulationResult.serializer(), response).yearlyResults
            return years.single { it.year == 2031 }.balances.tfa / years.single { it.year == 2030 }.balances.tfa
        }
        // The TFA has no withdrawals here, so its growth is the market return
        val crash = tfaGrowthIn2031(post("/api/simulate", body("crash-2008")).bodyAsText())
        val average = tfaGrowthIn2031(post("/api/simulate", body("historical-average")).bodyAsText())

        assertEquals(0.63, crash, 0.01)
        assertEquals(1.10, average, 0.01)
    }

    // --- Monte Carlo (US002-3, US002-4) ---

    private fun monteCarloBody(runs: Int, seed: Long? = 42) =
        Json.encodeToString(MonteCarloRequest.serializer(), MonteCarloRequest(testConfig(), runs, seed))

    @Test
    fun `monte carlo returns one path per run and the seed`() = testApplication {
        setup()
        val response = post("/api/simulate/monte-carlo", monteCarloBody(runs = 25))
        assertEquals(HttpStatusCode.OK, response.status)
        val mc = Json.decodeFromString(MonteCarloResult.serializer(), response.bodyAsText())
        assertEquals(42L, mc.seed)
        assertEquals(25, mc.paths.size)
    }

    @Test
    fun `monte carlo picks and returns a seed when none is sent`() = testApplication {
        setup()
        val mc = Json.decodeFromString(MonteCarloResult.serializer(), post("/api/simulate/monte-carlo", monteCarloBody(5, seed = null)).bodyAsText())
        val again = Json.decodeFromString(MonteCarloResult.serializer(), post("/api/simulate/monte-carlo",
            Json.encodeToString(MonteCarloRequest.serializer(), MonteCarloRequest(testConfig(), 5, mc.seed))).bodyAsText())
        assertEquals(mc.paths, again.paths)
        assertTrue(mc.seed in 0..9_007_199_254_740_991L, "seed survives a round trip through a JavaScript number")
    }

    @Test
    fun `monte carlo validates the config and the run count`() = testApplication {
        setup()
        val badConfig = testConfig().let { it.copy(rates = it.rates.copy(inflation = 3.0)) }
        val response = post("/api/simulate/monte-carlo",
            Json.encodeToString(MonteCarloRequest.serializer(), MonteCarloRequest(badConfig, runs = 0)))

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(listOf("rates.inflation", "runs"), errorFields(response.bodyAsText()))
    }

    @Test
    fun `run details match the run in the analysis`() = testApplication {
        setup()
        val mc = Json.decodeFromString(MonteCarloResult.serializer(), post("/api/simulate/monte-carlo", monteCarloBody(10)).bodyAsText())
        val response = post("/api/simulate/monte-carlo/run",
            Json.encodeToString(MonteCarloRunRequest.serializer(), MonteCarloRunRequest(testConfig(), mc.seed, 3)))

        assertEquals(HttpStatusCode.OK, response.status)
        val detail = Json.decodeFromString(MonteCarloRunDetail.serializer(), response.bodyAsText())
        assertEquals(mc.paths[3], detail.path)
        assertEquals(mc.years, detail.scenario.years)
    }

    @Test
    fun `run details reject a negative run index`() = testApplication {
        setup()
        val response = post("/api/simulate/monte-carlo/run",
            Json.encodeToString(MonteCarloRunRequest.serializer(), MonteCarloRunRequest(testConfig(), 1, -1)))
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(listOf("runIndex"), errorFields(response.bodyAsText()))
    }
}
