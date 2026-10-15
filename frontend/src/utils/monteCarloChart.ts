import { MonteCarloPath } from '../types/simulation';

/** Plot area of the chart in SVG pixels. */
export interface PlotArea {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface Scales {
  x: (year: number) => number;
  y: (balance: number) => number;
}

export function makeScales(plot: PlotArea, firstYear: number, lastYear: number, yMax: number): Scales {
  const span = Math.max(1, lastYear - firstYear);
  return {
    x: year => plot.x + ((year - firstYear) / span) * plot.width,
    y: balance => plot.y + plot.height - (balance / yMax) * plot.height
  };
}

/** SVG path data for one run; balances[i] is the balance at the end of firstYear + i. */
export function pathData(balances: number[], firstYear: number, scales: Scales): string {
  return balances
    .map((balance, i) => `${i === 0 ? 'M' : 'L'}${scales.x(firstYear + i).toFixed(1)},${scales.y(balance).toFixed(1)}`)
    .join('');
}

/**
 * Top of the balance axis: room for the confidence lines (the median is the highest of them) and the
 * starting balance. Returns are compounded over decades, so the best runs end tens of times higher than
 * the median; scaling to them would flatten the lines that matter. Runs above the top are clipped.
 */
export function chartYMax(medianPath: number[]): number {
  const raw = Math.max(2 * Math.max(0, ...medianPath), 1.5 * (medianPath[0] ?? 0), 1);
  // Round up to 1, 2 or 5 × a power of ten so the axis ticks are round numbers
  const magnitude = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 5, 10].find(m => m * magnitude >= raw)!;
  return step * magnitude;
}

/**
 * The run whose line passes closest to the pointer (vertically, at the pointer's year), if within
 * [maxDistance] pixels. Used for hover and click: 1,000 thin lines are too small to target one by one.
 */
export function nearestRun(
  paths: MonteCarloPath[],
  firstYear: number,
  scales: Scales,
  pointer: { x: number; y: number },
  maxDistance = 10
): number | null {
  // All runs share the years, so find the pointer's position between two years once
  const yearCount = Math.max(0, ...paths.map(p => p.totalBalances.length));
  let lo = 0;
  while (lo < yearCount - 2 && scales.x(firstYear + lo + 1) < pointer.x) lo++;
  const x0 = scales.x(firstYear + lo);
  const x1 = scales.x(firstYear + lo + 1);
  const t = Math.min(1, Math.max(0, (pointer.x - x0) / (x1 - x0)));

  let best: number | null = null;
  let bestDistance = maxDistance;
  for (const path of paths) {
    const balances = path.totalBalances;
    if (balances.length < 2 || pointer.x > scales.x(firstYear + balances.length - 1) + 1) continue; // run ended earlier
    const hi = Math.min(lo + 1, balances.length - 1);
    const y = scales.y(balances[lo] + (balances[hi] - balances[lo]) * t);
    const distance = Math.abs(y - pointer.y);
    if (distance < bestDistance) {
      bestDistance = distance;
      best = path.runIndex;
    }
  }
  return best;
}

/** $1.2M, $850K, $0 */
export function compactMoney(amount: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 }).format(amount);
}
