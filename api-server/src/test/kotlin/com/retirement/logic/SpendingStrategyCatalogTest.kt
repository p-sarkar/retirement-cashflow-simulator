package com.retirement.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The strategies the form offers (spec 006, FR006-002, research R006-7). */
class SpendingStrategyCatalogTest {

    @Test
    fun `the catalog lists the existing strategy first, then Risk-Based Guardrails`() {
        val all = SpendingStrategyCatalog.all
        assertEquals(listOf("PARTHA_V0_01_20250105", "RISK_BASED_GUARDRAILS_V1"), all.map { it.id })
        assertEquals("Partha's Spending Strategy-v0.01-20260105", all[0].name)
        assertEquals("Risk-Based Guardrails", all[1].name)
        assertEquals(listOf("NONE", "GUARDRAILS"), all.map { it.settings })
    }

    @Test
    fun `every entry has a name and a one-line description`() {
        SpendingStrategyCatalog.all.forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.description.isNotBlank())
            assertFalse('\n' in it.description, "${it.id}: description must be one line")
        }
    }

    @Test
    fun `find looks up a strategy by id`() {
        assertEquals("Risk-Based Guardrails", SpendingStrategyCatalog.find("RISK_BASED_GUARDRAILS_V1")?.name)
        assertNull(SpendingStrategyCatalog.find("NO_SUCH_STRATEGY"))
    }
}
