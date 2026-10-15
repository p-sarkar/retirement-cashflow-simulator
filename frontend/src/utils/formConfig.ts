import { MarketPreset, OneTimeExpense, SimulationConfig, StrategyConfig } from '../types/simulation';

/**
 * The form's copy of a config. The API treats defaulted fields as optional; the form always holds every
 * value. Rates and the TDA withdrawal percentage are whole-number percentages here (3 = 3%) and decimals
 * in the API (0.03); loan aprPercent is a whole number in both.
 */
export type FormConfig = SimulationConfig & {
  endAge: number;
  strategy: Required<StrategyConfig>;
  oneTimeExpenses: OneTimeExpense[];
  marketPresetId: string | null;
};

/** The market rates a preset sets; editing any of them switches the form to custom market conditions. */
export const MARKET_RATE_FIELDS = ['inflation', 'preRetirementGrowth', 'postRetirementGrowth', 'bondYield'] as const;

// 0.07 * 100 is 7.000000000000001 in floating point: round to the form's 2 decimal places
const toPercent = (decimal: number) => Math.round(decimal * 10000) / 100;

export function toApiConfig(form: FormConfig): SimulationConfig {
  return {
    ...form,
    rates: {
      inflation: form.rates.inflation / 100,
      preRetirementGrowth: form.rates.preRetirementGrowth / 100,
      postRetirementGrowth: form.rates.postRetirementGrowth / 100,
      bondYield: form.rates.bondYield / 100,
      hysaRate: form.rates.hysaRate / 100,
      incomeTax: form.rates.incomeTax / 100
    },
    strategy: {
      ...form.strategy,
      tdaWithdrawalPercentage: form.strategy.tdaWithdrawalPercentage / 100
    }
  };
}

/** A config from the API (e.g. a saved simulation) as form values, filling the API's defaults. */
export function toFormConfig(config: SimulationConfig): FormConfig {
  return {
    ...config,
    endAge: config.endAge ?? 85,
    contributions: { ...config.contributions, annualRoth: config.contributions.annualRoth ?? 0 },
    rates: {
      inflation: toPercent(config.rates.inflation),
      preRetirementGrowth: toPercent(config.rates.preRetirementGrowth),
      postRetirementGrowth: toPercent(config.rates.postRetirementGrowth),
      bondYield: toPercent(config.rates.bondYield),
      hysaRate: toPercent(config.rates.hysaRate),
      incomeTax: toPercent(config.rates.incomeTax)
    },
    strategy: {
      initialTdaWithdrawal: config.strategy.initialTdaWithdrawal,
      tdaWithdrawalPercentage: toPercent(config.strategy.tdaWithdrawalPercentage ?? 0.5),
      rothConversionAmount: config.strategy.rothConversionAmount ?? 0,
      rothConversionPreRetirement: config.strategy.rothConversionPreRetirement ?? 0,
      rothConversionPostRetirement: config.strategy.rothConversionPostRetirement ?? 0,
      type: config.strategy.type ?? 'PARTHA_V0_01_20250105'
    },
    oneTimeExpenses: config.oneTimeExpenses ?? [],
    marketPresetId: config.marketPresetId ?? null
  };
}

/** Fill the market rates from a preset (as form percentages) and remember which preset was chosen. */
export function applyPreset(form: FormConfig, preset: MarketPreset): FormConfig {
  return {
    ...form,
    rates: {
      ...form.rates,
      inflation: toPercent(preset.inflation),
      preRetirementGrowth: toPercent(preset.preRetirementGrowth),
      postRetirementGrowth: toPercent(preset.postRetirementGrowth),
      bondYield: toPercent(preset.bondYield)
    },
    marketPresetId: preset.id
  };
}
