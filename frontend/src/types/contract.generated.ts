// GENERATED FILE - DO NOT EDIT.
// Source: Kotlin @Serializable models in api-server (see TsContractGenerator).
// Regenerate: cd api-server && ./gradlew generateTsContract
// Optional (?) fields have server-side defaults and may be omitted in requests.

export interface SimulationConfig {
  id?: string | null;
  name: string;
  currentYear: number;
  currentAge: number;
  retirementAge: number;
  endAge?: number;
  salary: number;
  portfolio: Portfolio;
  spousal: SpousalDetails;
  expenses: ExpenseConfig;
  contributions: ContributionConfig;
  rates: RateConfig;
  strategy: StrategyConfig;
  oneTimeExpenses?: OneTimeExpense[];
  marketPresetId?: string | null;
}

export interface Portfolio {
  sb: number;
  cbb: number;
  tba: number;
  tda: number;
  tfa: number;
}

export interface SpousalDetails {
  spouseAge: number;
  lowerEarner: SocialSecurityDetails;
  higherEarner: SocialSecurityDetails;
}

export interface SocialSecurityDetails {
  claimAge: number;
  annualBenefit: number;
}

export interface ExpenseConfig {
  needs: number;
  wants: number;
  propertyTax: number;
  healthcarePreRetirement: number;
  healthcarePostRetirementPreMedicare: number;
  healthcareMedicare: number;
}

export interface ContributionConfig {
  annual401k: number;
  annualRoth?: number;
  annualTba: number;
}

export interface RateConfig {
  inflation: number;
  preRetirementGrowth: number;
  postRetirementGrowth: number;
  bondYield: number;
  hysaRate: number;
  incomeTax: number;
}

export interface StrategyConfig {
  initialTdaWithdrawal: number;
  tdaWithdrawalPercentage?: number;
  rothConversionAmount?: number;
  rothConversionPreRetirement?: number;
  rothConversionPostRetirement?: number;
  type?: string;
}

export type OneTimeExpense = CashExpense | LoanExpense;

export interface CashExpense {
  type: 'CASH';
  id: string;
  name: string;
  amount: number;
  yearOrAge: YearOrAge;
}

export type YearOrAge = YearOrAgeAge | YearOrAgeYear;

export interface YearOrAgeAge {
  type: 'AGE';
  age: number;
}

export interface YearOrAgeYear {
  type: 'YEAR';
  year: number;
}

export interface LoanExpense {
  type: 'LOAN';
  id: string;
  name: string;
  principal: number;
  aprPercent: number;
  termYears: number;
  startYearOrAge: YearOrAge;
  quarterlyPayment?: number | null;
  downPayment?: number;
}

export interface BreakdownRequest {
  config: SimulationConfig;
  targetAge: number;
}

export interface MonteCarloRequest {
  config: SimulationConfig;
  runs?: number;
  seed?: number | null;
}

export interface MonteCarloRunRequest {
  config: SimulationConfig;
  seed: number;
  runIndex: number;
}

export interface MonteCarloRunBreakdownRequest {
  config: SimulationConfig;
  seed: number;
  runIndex: number;
  targetAge: number;
}

export interface RenameSimulationRequest {
  name: string;
}

export interface SimulationResult {
  config: SimulationConfig;
  yearlyResults: YearlyResult[];
  quarterlyResults: QuarterlyResult[];
  summary: Summary;
  apiMetadata: ApiMetadata;
}

export interface YearlyResult {
  year: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
  oneTimeExpensesBreakdown: ExpenseDetail[] | null;
  market: MarketRates | null;
}

export interface CashFlow {
  salary: number;
  interest: number;
  dividends: number;
  socialSecurity: number;
  tbaWithdrawal: number;
  tdaWithdrawal: number;
  tdaWithdrawalSpend: number;
  tdaWithdrawalRoth: number;
  tfaWithdrawal: number;
  sbDeposit: number;
  sbWithdrawal: number;
  rothConversion: number;
  contribution401k: number;
  contributionRoth: number;
  contributionTba: number;
  totalIncome: number;
  needs: number;
  wants: number;
  healthcare: number;
  incomeTax: number;
  propertyTax: number;
  totalExpenses: number;
  oneTimeExpenses: number;
}

export interface Metrics {
  annualIncomeGap: number | null;
  incomeGapExpenses: number;
  incomeGapPassiveIncome: number;
  sbCap: number | null;
  cbbCap: number;
  isFailure: boolean;
}

export interface ExpenseDetail {
  name: string;
  amount: number;
  type: ExpenseType;
}

export type ExpenseType = 'CASH' | 'LOAN_PAYMENT';

export interface MarketRates {
  equityReturn: number;
  inflation: number;
  bondYield: number;
}

export interface QuarterlyResult {
  year: number;
  quarter: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
  market: MarketRates | null;
}

export interface Summary {
  finalTotalBalance: number;
  isSuccess: boolean;
  failureYear: number | null;
  totalDividends: number;
  totalInterest: number;
}

export interface ApiMetadata {
  version: string;
  buildTime: string;
  serverStartTime: string;
}

export interface YearBreakdown {
  year: number;
  age: number;
  spouseAge: number;
  phase: YearPhase;
  isLastYear: boolean;
  startingPosition: StartingPosition;
  expenses: ExpenseSection | null;
  income: IncomeSection | null;
  incomeGap: IncomeGapSection | null;
  moneyMovement: MoneyMovementSection | null;
  taxes: TaxSection | null;
  nextYear: NextYearSection | null;
  apiMetadata: ApiMetadata;
}

export type YearPhase = 'START' | 'WORKING' | 'RETIREMENT';

export interface StartingPosition {
  openingBalances: Portfolio;
  openingTotal: number;
  conditions: EconomicConditions;
  household: HouseholdStatus;
}

export interface EconomicConditions {
  inflationRate: number;
  cumulativeInflation: number;
  equityReturn: number;
  equityReturnSource: EquityReturnSource;
  presetName: string | null;
  presetSequenceYear: number | null;
  monteCarloRun: number | null;
  bondYield: number;
  hysaRate: number;
  incomeTaxRate: number;
  marketVsAllTimeHigh: number;
}

export type EquityReturnSource = 'PRE_RETIREMENT_RATE' | 'POST_RETIREMENT_RATE' | 'PRESET_SEQUENCE' | 'MONTE_CARLO_SCENARIO';

export interface HouseholdStatus {
  healthcareBracket: HealthcareBracket;
  earners: EarnerStatus[];
}

export type HealthcareBracket = 'PRE_RETIREMENT' | 'POST_RETIREMENT_PRE_MEDICARE' | 'MEDICARE';

export interface EarnerStatus {
  role: EarnerRole;
  age: number;
  claimAge: number;
  claiming: boolean;
}

export type EarnerRole = 'HIGHER' | 'LOWER';

export interface ExpenseSection {
  items: LineItem[];
  wantsDerivation: LineItem[];
  oneTimeExpenses: OneTimeExpenseItem[];
  total: number;
}

export interface LineItem {
  label: string;
  amount: number;
  rule: string;
  inputs: Record<string, number>;
  todaysDollars: number | null;
}

export interface OneTimeExpenseItem {
  name: string;
  type: OneTimeExpenseKind;
  amount: number;
  inflationAdjusted: boolean;
  todaysDollars: number | null;
}

export type OneTimeExpenseKind = 'CASH' | 'DOWN_PAYMENT' | 'LOAN_PAYMENT';

export interface IncomeSection {
  salary: SalaryBreakdown | null;
  interest: LineItem;
  dividends: LineItem;
  socialSecurity: EarnerBenefit[];
  incomeTotal: number;
  withdrawals: LineItem[];
  totalIncome: number;
  transfers: LineItem[];
}

export interface SalaryBreakdown {
  gross: number;
  contribution401k: number;
  contributionRoth: number;
  contributionTba: number;
  takeHome: number;
  todaysDollars: ConfiguredSalary;
}

export interface ConfiguredSalary {
  gross: number;
  contribution401k: number;
  contributionRoth: number;
  contributionTba: number;
}

export interface EarnerBenefit {
  role: EarnerRole;
  amount: number;
  spousalStepUp: boolean;
  startYear: number | null;
  todaysDollars: number | null;
}

export interface IncomeGapSection {
  estimated: GapComputation | null;
  actual: GapComputation | null;
  differences: LineItem[];
  capAig: LineItem | null;
  sbFloor: LineItem | null;
  sbCap: LineItem | null;
  cbbCap: LineItem | null;
  cbbCapReductions: number[];
  thresholds: StrategyThreshold[];
  takeHomeSalary: LineItem | null;
  workingExpenses: LineItem | null;
  explanation: string;
}

export interface GapComputation {
  expenses: number;
  passiveIncome: number;
  gap: number;
}

export interface StrategyThreshold {
  name: string;
  value: number;
  evaluated: boolean;
}

export interface MoneyMovementSection {
  ledgers: AccountLedger[];
  flows: Flow[];
  quarterStrategy: QuarterStrategy[];
  failure: FailureMark | null;
}

export interface AccountLedger {
  account: Endpoint;
  opening: number[];
  rows: LedgerRow[];
  closing: number[];
}

export type Endpoint = 'SB' | 'CBB' | 'TBA' | 'TDA' | 'TFA' | 'SALARY' | 'SOCIAL_SECURITY' | 'BANK' | 'BONDS' | 'EXPENSES' | 'MARKET';

export interface LedgerRow {
  type: MovementType;
  counterparty: Endpoint;
  amounts: number[];
  yearTotal: number;
}

export type MovementType = 'INTEREST' | 'DIVIDENDS' | 'TAKE_HOME_SALARY' | 'CONTRIBUTION_401K' | 'CONTRIBUTION_ROTH' | 'CONTRIBUTION_TBA' | 'SOCIAL_SECURITY' | 'REGULAR_EXPENSES' | 'ONE_TIME_EXPENSE' | 'STRATEGY_REFILL' | 'SHORTFALL_COVER' | 'ROTH_CONVERSION' | 'MARKET_GROWTH';

export interface Flow {
  from: Endpoint;
  to: Endpoint;
  amount: number;
}

export interface QuarterStrategy {
  quarter: number;
  rule: StrategyRule;
  marketVs12MonthsPrior: number;
  marketVsAllTimeHigh: number;
  refillToSb: number;
  refillToCbb: number;
  limitedBy: RefillLimit;
  cbbRefillLimitedBy: RefillLimit | null;
  cbbVs12MonthsPrior: number | null;
  cbbLoss: number | null;
  equityLoss: number | null;
  explanation: string;
}

export type StrategyRule = 'MARKET_UP_CBB_FULL' | 'MARKET_UP_CBB_REFILL' | 'MARKET_DOWN_HOLD' | 'MARKET_DOWN_FROM_CBB' | 'MARKET_DOWN_FROM_EQUITIES';

export type RefillLimit = 'QUARTERLY_SHARE' | 'SB_CAP' | 'SB_FLOOR_DEFICIT' | 'CBB_CAP' | 'AVAILABLE_BALANCE' | 'NONE';

export interface FailureMark {
  quarter: number;
  account: Endpoint;
  condition: string;
}

export interface TaxSection {
  billThisYear: TaxComputation;
  generatedThisYear: TaxComputation;
  simplifications: string[];
}

export interface TaxComputation {
  basis: TaxBasis;
  components: TaxComponent[];
  taxableIncome: number;
  rate: number;
  tax: number;
}

export type TaxBasis = 'PRIOR_YEAR' | 'START_YEAR_ESTIMATE' | 'THIS_YEAR';

export interface TaxComponent {
  name: string;
  amount: number;
  taxedShare: number;
  taxable: number;
}

export interface NextYearSection {
  year: number;
  age: number;
  expenses: ProjectedItem[];
  income: ProjectedItem[];
  incomeGap: ProjectedItem | null;
  changes: string[];
}

export interface ProjectedItem {
  label: string;
  amount: number;
  exact: boolean;
  change: number;
  rule: string;
}

export interface ValidationErrorResponse {
  error: string;
  statusCode: number;
  validationErrors: ValidationError[];
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface ErrorResponse {
  error: string;
  statusCode: number;
}

export interface MarketPreset {
  id: string;
  name: string;
  description: string;
  inflation: number;
  preRetirementGrowth: number;
  postRetirementGrowth: number;
  bondYield: number;
  retirementReturnSequence: number[];
}

export interface MonteCarloResult {
  seed: number;
  runCount: number;
  method: string;
  years: number[];
  paths: MonteCarloPath[];
  statistics: Statistics;
  apiMetadata: ApiMetadata;
}

export interface MonteCarloPath {
  runIndex: number;
  totalBalances: number[];
  isSuccess: boolean;
  failureYear: number | null;
  endingBalance: number;
  averageReturn: number;
  averageInflation: number;
  averageBondYield: number;
}

export interface Statistics {
  medianPath: number[];
  percentile75Path: number[];
  percentile90Path: number[];
  successRate: number;
}

export interface MonteCarloRunDetail {
  seed: number;
  path: MonteCarloPath;
  scenario: MarketScenario;
  result: SimulationResult;
}

export interface MarketScenario {
  years: number[];
  equityReturns: number[];
  inflationRates: number[];
  bondYields: number[];
}

export interface SavedSimulationSummary {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string | null;
  isSuccess: boolean;
  failureYear: number | null;
  finalTotalBalance: number;
  endAge: number;
  version: string | null;
  isCurrentVersion: boolean;
}

export interface SavedSimulation {
  summary: SavedSimulationSummary;
  result: SimulationResult;
  apiMetadata: ApiMetadata;
}

export interface SaveSimulationRequest {
  name: string;
  result: SimulationResult;
}

export interface UpdateSimulationRequest {
  result: SimulationResult;
}

export interface InputsFile {
  format: string;
  formatVersion: number;
  appVersion: string;
  config: SimulationConfig;
}
