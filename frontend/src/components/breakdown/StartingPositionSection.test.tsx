import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import StartingPositionSection from './StartingPositionSection';
import { StartingPosition } from '../../types/simulation';

const position = (overrides: Partial<StartingPosition['conditions']> = {}): StartingPosition => ({
  openingBalances: { sb: 162012, cbb: 508750, tba: 1723543, tda: 2350258, tfa: 721490 },
  openingTotal: 5466054,
  conditions: {
    inflationRate: 0.03, cumulativeInflation: 1.1941, equityReturn: 0.08, equityReturnSource: 'POST_RETIREMENT_RATE',
    presetName: null, presetSequenceYear: null, bondYield: 0.05, hysaRate: 0.04, incomeTaxRate: 0.12, marketVsAllTimeHigh: 1,
    ...overrides
  },
  household: {
    healthcareBracket: 'POST_RETIREMENT_PRE_MEDICARE',
    earners: [
      { role: 'HIGHER', age: 57, claimAge: 70, claiming: false },
      { role: 'LOWER', age: 64, claimAge: 62, claiming: true }
    ]
  }
});

describe('StartingPositionSection (US004-3)', () => {
  it('shows opening balances, the year\'s rates and the household', () => {
    render(<StartingPositionSection position={position()} />);

    expect(screen.getByText('$5,466,054')).toBeInTheDocument();
    expect(screen.getByText('× 1.1941')).toBeInTheDocument();
    expect(screen.getByText('Equity return from the configured post-retirement growth rate.')).toBeInTheDocument();
    expect(screen.getByText('Post-retirement, before Medicare')).toBeInTheDocument();
    expect(screen.getByText('Claims at 70')).toBeInTheDocument();
    expect(screen.getByText('Claiming Social Security')).toBeInTheDocument();
  });

  it('attributes a preset sequence year (FR004-011)', () => {
    render(<StartingPositionSection position={position({
      equityReturn: -0.37, equityReturnSource: 'PRESET_SEQUENCE', presetName: '2008 Crash', presetSequenceYear: 1
    })} />);

    expect(screen.getByText('-37%')).toBeInTheDocument();
    expect(screen.getByText('Equity return from the 2008 Crash preset, year 1 of its sequence.')).toBeInTheDocument();
  });
});
