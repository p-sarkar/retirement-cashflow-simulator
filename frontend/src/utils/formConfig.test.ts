import { describe, expect, it } from 'vitest';
import { applyPreset, toApiConfig, toFormConfig } from './formConfig';
import { apiConfig } from '../test/fixtures';
import { MarketPreset, SimulationConfig } from '../types/simulation';

describe('formConfig', () => {
  it('shows API decimals as form percentages without floating-point noise', () => {
    const form = toFormConfig(apiConfig);
    expect(form.rates).toEqual({
      inflation: 2.5, preRetirementGrowth: 7, postRetirementGrowth: 6, bondYield: 4.5, hysaRate: 3.5, incomeTax: 18
    });
    expect(form.strategy.tdaWithdrawalPercentage).toBe(70);
  });

  it('round-trips a saved config unchanged (SC-004)', () => {
    expect(toApiConfig(toFormConfig(apiConfig))).toEqual(apiConfig);
  });

  it('fills the API defaults for configs saved without them', () => {
    const old: SimulationConfig = { ...apiConfig, contributions: { annual401k: 1, annualTba: 2 }, strategy: { initialTdaWithdrawal: 0 } };
    delete old.endAge;
    delete old.oneTimeExpenses;
    delete old.marketPresetId;
    const form = toFormConfig(old);

    expect(form.endAge).toBe(85);
    expect(form.oneTimeExpenses).toEqual([]);
    expect(form.marketPresetId).toBeNull();
    expect(form.contributions.annualRoth).toBe(0);
    expect(form.strategy).toEqual({
      initialTdaWithdrawal: 0, tdaWithdrawalPercentage: 50, rothConversionAmount: 0, rothConversionPreRetirement: 0,
      rothConversionPostRetirement: 0, type: 'PARTHA_V0_01_20250105'
    });
  });

  it('applies a preset to the market rates only', () => {
    const preset: MarketPreset = {
      id: 'bull-market', name: 'Bull Market', description: '', inflation: 0.025, preRetirementGrowth: 0.12,
      postRetirementGrowth: 0.12, bondYield: 0.04, retirementReturnSequence: []
    };
    const form = applyPreset(toFormConfig(apiConfig), preset);

    expect(form.rates).toEqual({ inflation: 2.5, preRetirementGrowth: 12, postRetirementGrowth: 12, bondYield: 4, hysaRate: 3.5, incomeTax: 18 });
    expect(form.marketPresetId).toBe('bull-market');
  });
});
