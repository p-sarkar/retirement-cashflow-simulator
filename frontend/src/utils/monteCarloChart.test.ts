import { describe, expect, it } from 'vitest';
import { chartYMax, compactMoney, makeScales, nearestRun, pathData } from './monteCarloChart';
import { monteCarloPath } from '../test/fixtures';

// 2025-2028 across 300px, $0-$2M across 200px
const scales = makeScales({ x: 0, y: 0, width: 300, height: 200 }, 2025, 2028, 2000000);
const paths = [
  monteCarloPath(0, [1000000, 1200000, 1400000, 1600000]),
  monteCarloPath(1, [1000000, 1000000, 1000000, 1000000]),
  monteCarloPath(2, [1000000, 600000, 200000], false)
];

describe('Monte Carlo chart geometry', () => {
  it('maps years and balances to pixels', () => {
    expect(scales.x(2025)).toBe(0);
    expect(scales.x(2028)).toBe(300);
    expect(scales.y(0)).toBe(200);
    expect(scales.y(2000000)).toBe(0);
  });

  it('draws a run as a polyline', () => {
    expect(pathData([1000000, 2000000], 2025, scales)).toBe('M0.0,100.0L100.0,0.0');
  });

  it('finds the run nearest the pointer', () => {
    expect(nearestRun(paths, 2025, scales, { x: 200, y: 61 })).toBe(0); // run 0 is at y=60 in 2027
    expect(nearestRun(paths, 2025, scales, { x: 150, y: 100 })).toBe(1);
    expect(nearestRun(paths, 2025, scales, { x: 200, y: 180 })).toBe(2);
  });

  it('ignores the pointer when no run is close, or after a run ended', () => {
    expect(nearestRun(paths, 2025, scales, { x: 150, y: 30 })).toBeNull();
    expect(nearestRun(paths, 2025, scales, { x: 290, y: 180 })).toBeNull(); // run 2 failed in 2027
  });

  it('fits the axis to the median line and the start, rounded up', () => {
    expect(chartYMax([1000000, 1000000, 1000000, 1000000])).toBe(2000000);
    expect(chartYMax([4000000, 5300000, 1300000, 0])).toBe(20000000); // 2 x $5.3M
    expect(chartYMax([4000000, 0, 0])).toBe(10000000); // 1.5 x the start when the median collapses
    expect(chartYMax([0, 0])).toBe(1);
  });

  it('formats balances compactly', () => {
    expect(compactMoney(1234567)).toBe('$1.2M');
    expect(compactMoney(0)).toBe('$0');
  });
});
