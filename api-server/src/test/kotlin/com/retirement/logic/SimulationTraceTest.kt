package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import com.retirement.testConfig
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** T004-003: the simulation trace changes nothing (FR004-006) and records every balance change (SC004-001). */
class SimulationTraceTest {

    private val configs = listOf(testConfig(), defaultFormConfig()) + presetConfigs()

    @Test
    fun `a traced run returns the same result as an untraced one`() {
        configs.forEach { config ->
            val untraced = SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList())
            val (traced, _) = runTraced(config)
            assertEquals(untraced.copy(apiMetadata = traced.apiMetadata), traced, config.name)
        }
    }

    @Test
    fun `replaying the movements reproduces every quarterly balance`() {
        configs.forEach { config ->
            val (result, trace) = runTraced(config)
            val balances = mutableMapOf(
                Endpoint.SB to config.portfolio.sb, Endpoint.CBB to config.portfolio.cbb, Endpoint.TBA to config.portfolio.tba,
                Endpoint.TDA to config.portfolio.tda, Endpoint.TFA to config.portfolio.tfa
            )
            result.quarterlyResults.drop(1).forEach { q ->
                trace.years.getValue(q.year).movements.filter { it.quarter == q.quarter + 1 }.forEach { m ->
                    balances.computeIfPresent(m.from) { _, b -> b - m.amount }
                    balances.computeIfPresent(m.to) { _, b -> b + m.amount }
                }
                val expected = mapOf(
                    Endpoint.SB to q.balances.sb, Endpoint.CBB to q.balances.cbb, Endpoint.TBA to q.balances.tba,
                    Endpoint.TDA to q.balances.tda, Endpoint.TFA to q.balances.tfa
                )
                expected.forEach { (account, balance) ->
                    assertTrue(
                        abs(balances.getValue(account) - balance) < 0.01,
                        "${config.name} ${q.year} Q${q.quarter + 1} $account: replayed ${balances[account]}, engine $balance"
                    )
                }
            }
        }
    }

    @Test
    fun `movements are positive, between different endpoints, in quarters 1 to 4`() {
        configs.forEach { config ->
            runTraced(config).second.years.values.flatMap { it.movements }.forEach { m ->
                assertTrue(m.amount > 0.0, "$m")
                assertTrue(m.from != m.to, "$m")
                assertTrue(m.quarter in 1..4, "$m")
            }
        }
    }

    @Test
    fun `every simulated year has a plan and conditions`() {
        val (result, trace) = runTraced(defaultFormConfig())
        assertEquals(result.yearlyResults.drop(1).map { it.year }, trace.years.keys.sorted())
        assertTrue(trace.startYearTaxableIncomeEstimate > 0.0)
    }

    @Test
    fun `a failing plan records the first failing quarter`() {
        // T002-043.1: zero balances, the plan fails once retired
        val config = testConfig(portfolio = Portfolio(0.0, 0.0, 0.0, 0.0, 0.0)).copy(salary = 150000.0)
        val (result, trace) = runTraced(config)
        val failure = assertNotNull(trace.years.getValue(result.summary.failureYear!!).failure)
        assertTrue(failure.quarter in 1..4)
        assertTrue(failure.condition.isNotBlank())
        assertTrue(trace.years.filterKeys { it < result.summary.failureYear!! }.values.all { it.failure == null })
    }
}
