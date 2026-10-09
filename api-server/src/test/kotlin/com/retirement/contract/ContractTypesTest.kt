package com.retirement.contract

import kotlin.test.Test
import kotlin.test.assertTrue

class ContractTypesTest {

    private val generated = TsContractGenerator.generate()

    @Test
    fun `generated TypeScript contract files are up to date`() {
        TsContractGenerator.outputFiles.forEach { file ->
            val actual = if (file.exists()) file.readText() else ""
            assertTrue(
                actual == generated,
                "${file.path} is out of date with the Kotlin models. Run: ${TsContractGenerator.REGENERATE_COMMAND}"
            )
        }
    }

    @Test
    fun `request defaults are optional, response fields are always present`() {
        // LoanExpense is sent in requests: defaulted fields may be omitted
        assertTrue("  quarterlyPayment?: number | null;" in generated)
        assertTrue("  downPayment?: number;" in generated)
        // CashFlow / YearlyResult only appear in responses: Ktor always encodes them
        assertTrue("  oneTimeExpenses: number;" in generated)
        assertTrue("  oneTimeExpensesBreakdown: ExpenseDetail[] | null;" in generated)
    }

    @Test
    fun `sealed hierarchies become discriminated unions`() {
        assertTrue("export type OneTimeExpense = CashExpense | LoanExpense;" in generated)
        assertTrue("export interface CashExpense {\n  type: 'CASH';" in generated)
        assertTrue("export type YearOrAge = YearOrAgeAge | YearOrAgeYear;" in generated)
        assertTrue("export type ExpenseType = 'CASH' | 'LOAN_PAYMENT';" in generated)
    }
}
