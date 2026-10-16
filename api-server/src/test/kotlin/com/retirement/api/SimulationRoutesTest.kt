package com.retirement.api

import com.retirement.model.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationRoutesTest {

    private fun config(vararg expenses: OneTimeExpense) = SimulationConfig(
        name = "Routes Test",
        currentYear = 2025,
        currentAge = 60,
        retirementAge = 65,
        salary = 120000.0,
        portfolio = Portfolio(sb = 200000.0, cbb = 1000000.0, tba = 500000.0, tda = 1000000.0, tfa = 0.0),
        spousal = SpousalDetails(58, SocialSecurityDetails(67, 20000.0), SocialSecurityDetails(70, 40000.0)),
        expenses = ExpenseConfig(60000.0, 30000.0, 10000.0, 5000.0, 5000.0, 3000.0),
        contributions = ContributionConfig(annual401k = 23000.0, annualTba = 12000.0),
        rates = RateConfig(0.03, 0.07, 0.05, 0.04, 0.04, 0.20),
        strategy = StrategyConfig(initialTdaWithdrawal = 40000.0, rothConversionAmount = 10000.0),
        oneTimeExpenses = expenses.toList()
    )

    private val validLoan = LoanExpense(
        id = "l", name = "Car Loan", principal = 40000.0, aprPercent = 5.0, termYears = 5,
        startYearOrAge = YearOrAge.Year(2027)
    )
    private val validCash = CashExpense(id = "c", name = "Roof", amount = 20000.0, yearOrAge = YearOrAge.Year(2028))

    private fun ApplicationTestBuilder.setup() {
        install(ContentNegotiation) { json() }
        routing { simulationRoutes() }
    }

    private suspend fun ApplicationTestBuilder.post(path: String, body: String) =
        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private fun simulateBody(c: SimulationConfig) = Json.encodeToString(SimulationConfig.serializer(), c)

    private fun breakdownBody(c: SimulationConfig, age: Int) =
        Json.encodeToString(BreakdownRequest.serializer(), BreakdownRequest(c, age))

    private fun errorFields(response: String): List<String> =
        Json.parseToJsonElement(response).jsonObject["validationErrors"]!!.jsonArray
            .map { it.jsonObject["field"]!!.jsonPrimitive.content }

    @Test
    fun `valid config simulates successfully`() = testApplication {
        setup()
        val response = post("/api/simulate", simulateBody(config(validCash, validLoan)))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `invalid expense returns 422 with structured field errors`() = testApplication {
        setup()
        val response = post("/api/simulate", simulateBody(config(validLoan.copy(termYears = 0))))

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Validation failed", json["error"]!!.jsonPrimitive.content)
        assertEquals(422, json["statusCode"]!!.jsonPrimitive.int)
        assertEquals(listOf("oneTimeExpenses[0].termYears"), errorFields(response.bodyAsText()))
    }

    @Test
    fun `errors from multiple expenses are all reported with their indexes`() = testApplication {
        setup()
        val response = post(
            "/api/simulate",
            simulateBody(config(validCash.copy(amount = -1.0), validLoan, validLoan.copy(aprPercent = -2.0)))
        )

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(
            listOf("oneTimeExpenses[0].amount", "oneTimeExpenses[2].aprPercent"),
            errorFields(response.bodyAsText())
        )
    }

    @Test
    fun `rates sent as whole-number percentages return 422`() = testApplication {
        setup()
        val wrongUnits = config(validLoan).let { it.copy(rates = it.rates.copy(inflation = 3.0)) }
        val response = post("/api/simulate", simulateBody(wrongUnits))

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(listOf("rates.inflation"), errorFields(response.bodyAsText()))
    }

    @Test
    fun `breakdown route also rejects invalid expenses`() = testApplication {
        setup()
        val response = post("/api/simulate/breakdown", breakdownBody(config(validLoan.copy(downPayment = 50000.0)), 62))

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(listOf("oneTimeExpenses[0].downPayment"), errorFields(response.bodyAsText()))
    }

    @Test
    fun `breakdown route returns a YearBreakdown for valid config`() = testApplication {
        setup()
        val response = post("/api/simulate/breakdown", breakdownBody(config(validLoan), 62))
        assertEquals(HttpStatusCode.OK, response.status)
        val breakdown = Json { ignoreUnknownKeys = true }.decodeFromString(YearBreakdown.serializer(), response.bodyAsText())
        assertEquals(62, breakdown.age)
        assertEquals(YearPhase.WORKING, breakdown.phase)
        assertTrue(breakdown.startingPosition.openingTotal > 0.0)
    }

    @Test
    fun `breakdown route returns a JSON 400 for an age outside the simulation`() = testApplication {
        setup()
        val response = post("/api/simulate/breakdown", breakdownBody(config(validLoan), 99))
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject["error"] != null, response.bodyAsText())
    }
}
