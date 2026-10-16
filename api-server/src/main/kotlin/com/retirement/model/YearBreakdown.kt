package com.retirement.model

import kotlinx.serialization.Serializable

/**
 * Computation breakdown for one simulated year (spec 004): seven sections that follow the year in order.
 * All amounts are dollars of the year shown; rates are decimals (0.03 = 3%).
 */
@Serializable
data class YearBreakdown(
    val year: Int,
    val age: Int,
    val spouseAge: Int,
    val phase: YearPhase,
    val isLastYear: Boolean,
    val startingPosition: StartingPosition,
    val expenses: ExpenseSection? = null,
    val income: IncomeSection? = null,
    val incomeGap: IncomeGapSection? = null,
    val moneyMovement: MoneyMovementSection? = null,
    val taxes: TaxSection? = null,
    val nextYear: NextYearSection? = null, // null for the start year and the last simulated year (FR004-073)
    val apiMetadata: ApiMetadata
)

/** START is the start-year snapshot: only `startingPosition` is set. */
@Serializable
enum class YearPhase { START, WORKING, RETIREMENT }

/** A labelled amount and how it was computed (FR004-003). */
@Serializable
data class LineItem(
    val label: String,
    val amount: Double,
    val rule: String,
    val inputs: Map<String, Double> = emptyMap(),
    val todaysDollars: Double? = null // the configured amount before inflation adjustment (FR004-005)
)

// --- Section 1: Starting Position (FR004-010–FR004-013) ---

@Serializable
data class StartingPosition(
    val openingBalances: Portfolio,
    val openingTotal: Double,
    val conditions: EconomicConditions,
    val household: HouseholdStatus
)

@Serializable
data class EconomicConditions(
    val inflationRate: Double,
    val cumulativeInflation: Double, // factor since the start year
    val equityReturn: Double,
    val equityReturnSource: EquityReturnSource,
    val presetName: String? = null,
    val presetSequenceYear: Int? = null, // 1-based year of the preset's return sequence
    val bondYield: Double,
    val hysaRate: Double,
    val incomeTaxRate: Double,
    val marketVsAllTimeHigh: Double // at the start of the year
)

@Serializable
enum class EquityReturnSource { PRE_RETIREMENT_RATE, POST_RETIREMENT_RATE, PRESET_SEQUENCE }

@Serializable
data class HouseholdStatus(
    val healthcareBracket: HealthcareBracket,
    val earners: List<EarnerStatus>
)

@Serializable
enum class HealthcareBracket { PRE_RETIREMENT, POST_RETIREMENT_PRE_MEDICARE, MEDICARE }

@Serializable
data class EarnerStatus(
    val role: EarnerRole,
    val age: Int,
    val claimAge: Int,
    val claiming: Boolean
)

@Serializable
enum class EarnerRole { HIGHER, LOWER }

// --- Section 2: Expenses (FR004-020–FR004-022) ---

@Serializable
data class ExpenseSection(
    val items: List<LineItem>, // Needs, Wants, Healthcare, Property Tax, Income Tax
    val wantsDerivation: List<LineItem> = emptyList(), // working years: salary, each deduction, the result
    val oneTimeExpenses: List<OneTimeExpenseItem> = emptyList(),
    val total: Double // = results-table Total Expenses
)

@Serializable
data class OneTimeExpenseItem(
    val name: String,
    val type: OneTimeExpenseKind,
    val amount: Double,
    val inflationAdjusted: Boolean // loan payments are fixed for the term
)

@Serializable
enum class OneTimeExpenseKind { CASH, DOWN_PAYMENT, LOAN_PAYMENT }

// --- Section 3: Income (FR004-030–FR004-032) ---

@Serializable
data class IncomeSection(
    val salary: SalaryBreakdown? = null, // null in retirement years
    val interest: LineItem,
    val dividends: LineItem,
    val socialSecurity: List<EarnerBenefit>,
    val incomeTotal: Double, // salary + interest + dividends + Social Security
    val withdrawals: List<LineItem>, // TBA withdrawals, TDA withdrawals for spending
    val totalIncome: Double, // = results-table Total Income = incomeTotal + withdrawals
    val transfers: List<LineItem> = emptyList() // Roth conversions: between accounts, not income (#16)
)

@Serializable
data class SalaryBreakdown(
    val gross: Double,
    val contribution401k: Double,
    val contributionRoth: Double,
    val contributionTba: Double,
    val takeHome: Double
)

@Serializable
data class EarnerBenefit(
    val role: EarnerRole,
    val amount: Double,
    val spousalStepUp: Boolean,
    val startYear: Int? = null // null if the earner never claims within the simulation
)

// --- Section 4: Income Gap (FR004-040–FR004-042) ---

@Serializable
data class IncomeGapSection(
    val estimated: GapComputation? = null, // retirement years: the gap the spending strategy planned for
    val actual: GapComputation? = null, // retirement years: the gap at year end (= results-table Income Gap)
    val differences: List<LineItem> = emptyList(), // estimated vs actual
    val capAig: LineItem? = null,
    val sbFloor: LineItem? = null,
    val sbCap: LineItem? = null,
    val cbbCap: LineItem? = null,
    val cbbCapReductions: List<Int> = emptyList(), // milestone ages reached
    val thresholds: List<StrategyThreshold> = emptyList(),
    val takeHomeSalary: LineItem? = null, // working years
    val workingExpenses: LineItem? = null, // working years
    val explanation: String
)

@Serializable
data class GapComputation(
    val expenses: Double,
    val passiveIncome: Double,
    val gap: Double
)

@Serializable
data class StrategyThreshold(
    val name: String,
    val value: Double,
    val evaluated: Boolean // false for the CBB-performance check, which the engine doesn't evaluate yet
)

// --- Section 5: Money Movement (FR004-050–FR004-055) ---

@Serializable
data class MoneyMovementSection(
    val ledgers: List<AccountLedger>, // SB, CBB, TBA, TDA, TFA
    val flows: List<Flow>,
    val quarterStrategy: List<QuarterStrategy> = emptyList(), // retirement years: one per quarter
    val failure: FailureMark? = null
)

@Serializable
data class AccountLedger(
    val account: Endpoint,
    val opening: List<Double>, // Q1–Q4
    val rows: List<LedgerRow>,
    val closing: List<Double> // Q1–Q4; closing[3] = the results-table year-end balance
)

@Serializable
data class LedgerRow(
    val type: MovementType,
    val counterparty: Endpoint,
    val amounts: List<Double>, // Q1–Q4, + into the account, − out of it
    val yearTotal: Double
)

@Serializable
data class Flow(
    val from: Endpoint,
    val to: Endpoint,
    val amount: Double
)

@Serializable
enum class MovementType {
    INTEREST, DIVIDENDS, TAKE_HOME_SALARY, CONTRIBUTION_401K, CONTRIBUTION_ROTH, CONTRIBUTION_TBA, SOCIAL_SECURITY,
    REGULAR_EXPENSES, ONE_TIME_EXPENSE, STRATEGY_REFILL, SHORTFALL_COVER, ROTH_CONVERSION, MARKET_GROWTH
}

/** The five accounts and the outside parties money moves to and from. */
@Serializable
enum class Endpoint {
    SB, CBB, TBA, TDA, TFA, SALARY, SOCIAL_SECURITY, BANK, BONDS, EXPENSES, MARKET;

    val isAccount: Boolean get() = this in setOf(SB, CBB, TBA, TDA, TFA)
}

@Serializable
data class QuarterStrategy(
    val quarter: Int,
    val rule: StrategyRule,
    val marketVs12MonthsPrior: Double,
    val marketVsAllTimeHigh: Double,
    val refillToSb: Double,
    val refillToCbb: Double,
    val limitedBy: RefillLimit,
    val explanation: String = ""
)

@Serializable
enum class StrategyRule { MARKET_UP_CBB_FULL, MARKET_UP_CBB_REFILL, MARKET_DOWN_HOLD, MARKET_DOWN_FROM_CBB }

@Serializable
enum class RefillLimit { QUARTERLY_SHARE, SB_CAP, SB_FLOOR_DEFICIT, CBB_CAP, AVAILABLE_BALANCE, NONE }

@Serializable
data class FailureMark(
    val quarter: Int,
    val account: Endpoint,
    val condition: String
)

// --- Section 6: Taxes (FR004-060–FR004-062) ---

@Serializable
data class TaxSection(
    val billThisYear: TaxComputation,
    val generatedThisYear: TaxComputation, // sets next year's bill
    val simplifications: List<String>
)

@Serializable
data class TaxComputation(
    val basis: TaxBasis,
    val components: List<TaxComponent>,
    val taxableIncome: Double,
    val rate: Double,
    val tax: Double
)

@Serializable
enum class TaxBasis { PRIOR_YEAR, START_YEAR_ESTIMATE, THIS_YEAR }

@Serializable
data class TaxComponent(
    val name: String,
    val amount: Double,
    val taxedShare: Double,
    val taxable: Double
)

// --- Section 7: Next Year (FR004-070–FR004-073) ---

@Serializable
data class NextYearSection(
    val year: Int,
    val age: Int,
    val expenses: List<ProjectedItem>,
    val income: List<ProjectedItem>,
    val incomeGap: ProjectedItem? = null, // null when next year is a working year
    val changes: List<String>
)

@Serializable
data class ProjectedItem(
    val label: String,
    val amount: Double,
    val exact: Boolean, // false for amounts that depend on market returns (estimates)
    val change: Double // vs this year
)
