# Data Model

## Entities

### SimulationConfig
Represents the user input configuration for a simulation run.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier (optional, for saved runs) |
| `name` | String | User-defined name for the simulation |
| `currentYear` | Integer | The starting year of the simulation |
| `currentAge` | Integer | Primary user's current age |
| `retirementAge` | Integer | Age at which retirement begins |
| `portfolio` | `Portfolio` | Current account balances |
| `spousal` | `SpousalDetails` | Spouse-related configuration |
| `expenses` | `ExpenseConfig` | Initial expense estimates |
| `rates` | `RateConfig` | Assumed growth and tax rates |
| `strategy` | `StrategyConfig` | Spending strategy parameters |

### Portfolio
| Field | Type | Description |
|-------|------|-------------|
| `sb` | Decimal | Spend Bucket (HYSA) current balance |
| `cbb` | Decimal | Crash Buffer Bucket (Bonds) current balance |
| `tba` | Decimal | Taxable Brokerage Account balance |
| `tda` | Decimal | Tax-Deferred Account balance |
| `tfa` | Decimal | Tax-Free Account balance |

### SpousalDetails
| Field | Type | Description |
|-------|------|-------------|
| `spouseAge` | Integer | Spouse's current age |
| `lowerEarner` | `SocialSecurityDetails` | Details for lower earner |
| `higherEarner` | `SocialSecurityDetails` | Details for higher earner |

### SocialSecurityDetails
| Field | Type | Description |
|-------|------|-------------|
| `claimAge` | Integer | Age to start claiming SS |
| `annualBenefit` | Decimal | Annual benefit amount at claim age |

### ExpenseConfig
| Field | Type | Description |
|-------|------|-------------|
| `needs` | Decimal | Annual essential expenses |
| `wants` | Decimal | Annual discretionary expenses |
| `propertyTax` | Decimal | Annual property tax |
| `healthcarePreMedicare` | Decimal | Annual cost before 65 |
| `healthcareMedicare` | Decimal | Annual cost at 65+ |

### RateConfig
| Field | Type | Description |
|-------|------|-------------|
| `inflation` | Decimal | Annual inflation rate (e.g., 0.03) |
| `preRetirementGrowth` | Decimal | Equities growth before retirement |
| `postRetirementGrowth` | Decimal | Equities growth after retirement |
| `bondYield` | Decimal | Annual bond yield |
| `hysaRate` | Decimal | HYSA interest rate |
| `incomeTax` | Decimal | Effective income tax rate |

### StrategyConfig
| Field | Type | Description |
|-------|------|-------------|
| `initialTdaWithdrawal` | Decimal | Initial annual withdrawal from TDA |
| `type` | String | "PARTHA_V0_01" |

### SimulationResult
The output of a simulation.

| Field | Type | Description |
|-------|------|-------------|
| `config` | `SimulationConfig` | The input configuration used |
| `yearlyResults` | List<`YearlyResult`> | Year-by-year breakdown |
| `summary` | `Summary` | Final status and aggregates |

### YearlyResult
| Field | Type | Description |
|-------|------|-------------|
| `year` | Integer | Calendar year |
| `age` | Integer | User's age |
| `balances` | `Portfolio` | Ending balances for the year |
| `cashFlow` | `CashFlow` | Income and Expenses for the year |
| `metrics` | `Metrics` | Calculated metrics (AIG, etc.) |

### CashFlow
| Field | Type | Description |
|-------|------|-------------|
| `salary` | Decimal | |
| `interest` | Decimal | From SB |
| `dividends` | Decimal | From CBB |
| `socialSecurity` | Decimal | Total SS benefit |
| `tbaWithdrawal` | Decimal | |
| `tdaWithdrawal` | Decimal | |
| `totalIncome` | Decimal | |
| `needs` | Decimal | Inflation adjusted |
| `wants` | Decimal | Inflation adjusted |
| `healthcare` | Decimal | |
| `taxes` | Decimal | Income + Property |
| `totalExpenses` | Decimal | |

### Metrics
| Field | Type | Description |
|-------|------|-------------|
| `annualIncomeGap` | Decimal | (Expenses - Recurring Income) |
| `isFailure` | Boolean | True if any balance <= 0 |

### MonteCarloResult
| Field | Type | Description |
|-------|------|-------------|
| `runs` | List<`SimulationRun`> | Only summary data per run usually |
| `statistics` | `Statistics` | Median, 75th, 90th percentiles |