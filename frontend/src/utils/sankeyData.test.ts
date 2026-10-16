import { describe, expect, it } from 'vitest';
import { toSankeyData } from './sankeyData';

describe('toSankeyData (FR004-055)', () => {
  it('makes one node per endpoint used and one link per flow', () => {
    const data = toSankeyData([
      { from: 'SALARY', to: 'SB', amount: 200000 },
      { from: 'SB', to: 'EXPENSES', amount: 180000 },
      { from: 'MARKET', to: 'TDA', amount: 50000 }
    ]);

    expect(data.nodes.map(n => n.name)).toEqual(['Salary', 'Spend Bucket', 'Expenses', 'Market', 'Tax Deferred']);
    expect(data.links).toEqual([
      { source: 0, target: 1, value: 200000 },
      { source: 1, target: 2, value: 180000 },
      { source: 3, target: 4, value: 50000 }
    ]);
  });

  it('is empty without flows', () => {
    expect(toSankeyData([])).toEqual({ nodes: [], links: [] });
  });
});
