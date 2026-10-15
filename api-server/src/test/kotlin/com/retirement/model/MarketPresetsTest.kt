package com.retirement.model

import com.retirement.logic.ConfigValidator
import com.retirement.testConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarketPresetsTest {

    @Test
    fun `presets include the spec's examples with unique ids`() {
        val names = MarketPresets.all.map { it.name }
        assertTrue(names.containsAll(listOf("Historical Average", "2008 Crash", "Bull Market")))
        assertEquals(MarketPresets.all.size, MarketPresets.all.map { it.id }.toSet().size)
    }

    @Test
    fun `preset rates pass config validation`() {
        MarketPresets.all.forEach { preset ->
            val config = testConfig(
                rates = testConfig().rates.copy(
                    inflation = preset.inflation, preRetirementGrowth = preset.preRetirementGrowth,
                    postRetirementGrowth = preset.postRetirementGrowth, bondYield = preset.bondYield
                ),
                marketPresetId = preset.id
            )
            assertTrue(ConfigValidator.validate(config).isValid, preset.id)
        }
    }

    @Test
    fun `no preset or a preset without a sequence uses the static rates`() {
        assertEquals(emptyList(), MarketPresets.equityReturns(testConfig()))
        assertEquals(emptyList(), MarketPresets.equityReturns(testConfig(marketPresetId = "historical-average")))
    }

    @Test
    fun `the 2008 crash sequence starts in the first retirement year`() {
        // Age 60 in 2025, retiring at 65: 2030 (age 65) is the last working year, 2031 (age 66) the first retired
        val returns = MarketPresets.equityReturns(testConfig(marketPresetId = "crash-2008"))
        val crash = assertNotNull(MarketPresets.find("crash-2008")).retirementReturnSequence

        assertEquals(26, returns.size) // start year (index 0) through age 85
        assertEquals(List(6) { 0.10 }, returns.subList(0, 6)) // ages 60-65: pre-retirement growth
        assertEquals(crash, returns.subList(6, 11)) // ages 66-70
        assertEquals(List(15) { 0.10 }, returns.subList(11, 26)) // then post-retirement growth
    }

    @Test
    fun `already retired, the sequence starts in the first simulated year`() {
        val returns = MarketPresets.equityReturns(testConfig(currentAge = 70, retirementAge = 65, marketPresetId = "crash-2008"))
        assertEquals(-0.37, returns[1])
    }

    @Test
    fun `an unknown preset id is rejected`() {
        val errors = ConfigValidator.validate(testConfig(marketPresetId = "dot-com")).errors
        assertEquals(listOf("marketPresetId"), errors.map { it.field })
    }
}
