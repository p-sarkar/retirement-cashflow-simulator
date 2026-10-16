import React, { memo, useMemo, useState } from 'react';
import { Box, Typography } from '@mui/material';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, XAxis, YAxis, usePlotArea } from 'recharts';
import { MonteCarloPath, MonteCarloResult } from '../types/simulation';
import { chartYMax, compactMoney, makeScales, nearestRun, pathData, PlotArea } from '../utils/monteCarloChart';

interface MonteCarloChartProps {
  result: MonteCarloResult;
  selectedRunIndex: number | null;
  onSelectRun: (runIndex: number) => void;
  /** Fixed size for tests (jsdom has no layout); the page lets the chart fill its container. */
  width?: number;
  height?: number;
}

// Emphasis: every run in gray (failed runs in a light status red), the confidence lines in categorical
// slots 1-3, the hovered and selected runs in ink. Each line also has its own dash pattern and an end label.
const COLORS = {
  run: '#9e9e9e',
  failedRun: '#e57373',
  hovered: '#52514e',
  selected: '#0b0b0b'
};

export const CONFIDENCE_LINES = [
  { key: 'median', label: 'Median', color: '#2a78d6', dash: undefined },
  { key: 'p75', label: '75% of runs above', color: '#eb6834', dash: '6 3' },
  { key: 'p90', label: '90% of runs above', color: '#1baf7a', dash: '2 3' }
] as const;

interface LayerProps {
  paths: MonteCarloPath[];
  firstYear: number;
  lastYear: number;
  yMax: number;
}

const clipId = 'monte-carlo-plot-clip';

/** All runs as one SVG layer, drawn once: hovering redraws only the highlight layer. */
const RunPaths = memo(function RunPaths({ paths, firstYear, lastYear, yMax }: LayerProps) {
  const plot = usePlotArea();
  if (!plot) return null;
  const scales = makeScales(plot, firstYear, lastYear, yMax);
  return (
    <g clipPath={`url(#${clipId})`} data-testid="run-paths">
      <defs>
        <clipPath id={clipId}><rect x={plot.x} y={plot.y} width={plot.width} height={plot.height} /></clipPath>
      </defs>
      {paths.map(path => (
        <path
          key={path.runIndex}
          d={pathData(path.totalBalances, firstYear, scales)}
          fill="none"
          stroke={path.isSuccess ? COLORS.run : COLORS.failedRun}
          strokeOpacity={path.isSuccess ? 0.18 : 0.35}
          strokeWidth={1}
        />
      ))}
    </g>
  );
});

interface InteractionLayerProps extends LayerProps {
  hovered: MonteCarloPath | null;
  selected: MonteCarloPath | null;
  onHover: (runIndex: number | null) => void;
  onSelect: (runIndex: number) => void;
}

/** The hovered and selected runs, plus a transparent overlay that finds the run nearest the pointer. */
function InteractionLayer({ paths, firstYear, lastYear, yMax, hovered, selected, onHover, onSelect }: InteractionLayerProps) {
  const plot = usePlotArea();
  if (!plot) return null;
  const scales = makeScales(plot, firstYear, lastYear, yMax);

  const pointerIn = (e: React.MouseEvent<SVGRectElement>) => {
    const box = (e.currentTarget.ownerSVGElement ?? e.currentTarget).getBoundingClientRect();
    return { x: e.clientX - box.left, y: e.clientY - box.top };
  };
  const highlight = (path: MonteCarloPath, color: string, width: number, testId: string) => (
    <path d={pathData(path.totalBalances, firstYear, scales)} fill="none" stroke={color} strokeWidth={width}
      clipPath={`url(#${clipId})`} pointerEvents="none" data-testid={testId} />
  );

  return (
    <g>
      {hovered && hovered.runIndex !== selected?.runIndex && highlight(hovered, COLORS.hovered, 2, 'hovered-run')}
      {selected && highlight(selected, COLORS.selected, 3, 'selected-run')}
      <rect
        x={plot.x} y={plot.y} width={plot.width} height={plot.height}
        fill="transparent"
        style={{ cursor: hovered ? 'pointer' : 'default' }}
        data-testid="run-hit-area"
        onMouseMove={e => onHover(nearestRun(paths, firstYear, scales, pointerIn(e)))}
        onMouseLeave={() => onHover(null)}
        onClick={e => {
          const runIndex = nearestRun(paths, firstYear, scales, pointerIn(e));
          if (runIndex !== null) onSelect(runIndex);
        }}
      />
    </g>
  );
}

/** Label at the right end of each confidence line, in text ink beside a swatch of the line's color. */
function EndLabels({ rows, lastYear, firstYear, yMax }: { rows: ChartRow[]; firstYear: number; lastYear: number; yMax: number }) {
  const plot: PlotArea | undefined = usePlotArea();
  const last = rows[rows.length - 1];
  if (!plot || !last) return null;
  const scales = makeScales(plot, firstYear, lastYear, yMax);
  // Keep labels at least 14px apart, stacking upward from the lowest, so close lines (e.g. all at $0)
  // don't overlap each other or the axis below
  let previousY = Infinity;
  const labels = CONFIDENCE_LINES
    .map(line => ({ line, y: Math.min(plot.y + plot.height, Math.max(plot.y, scales.y(last[line.key]))) }))
    .sort((a, b) => b.y - a.y)
    .map(({ line, y }) => {
      const placed = Math.min(y, previousY - 14);
      previousY = placed;
      return { line, y: placed };
    });
  return (
    <g pointerEvents="none">
      {labels.map(({ line, y }) => (
        <g key={line.key} transform={`translate(${plot.x + plot.width + 6},${y})`}>
          <line x1={0} x2={10} y1={0} y2={0} stroke={line.color} strokeWidth={2} strokeDasharray={line.dash} />
          <text x={14} y={4} fontSize={11} fill="#52514e">{compactMoney(last[line.key])}</text>
        </g>
      ))}
    </g>
  );
}

interface ChartRow {
  year: number;
  median: number;
  p75: number;
  p90: number;
}

/**
 * Every Monte Carlo run's total balance by year, with the median, 75% and 90% confidence lines
 * (FR002-015 to FR002-017). Hover highlights the nearest run; click selects it.
 */
const MonteCarloChart: React.FC<MonteCarloChartProps> = ({ result, selectedRunIndex, onSelectRun, width, height = 420 }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const { years, paths, statistics } = result;
  const firstYear = years[0];
  const lastYear = years[years.length - 1];

  const rows: ChartRow[] = useMemo(() => years.map((year, i) => ({
    year,
    median: statistics.medianPath[i],
    p75: statistics.percentile75Path[i],
    p90: statistics.percentile90Path[i]
  })), [years, statistics]);
  const yMax = useMemo(() => chartYMax(statistics.medianPath), [statistics]);

  const hovered = hoveredIndex === null ? null : paths[hoveredIndex] ?? null;
  const selected = selectedRunIndex === null ? null : paths[selectedRunIndex] ?? null;

  const chart = (
    <LineChart data={rows} width={width} height={width ? height : undefined} margin={{ top: 10, right: 70, bottom: 5, left: 10 }}>
      <CartesianGrid stroke="#e8e8e6" vertical={false} />
      <XAxis dataKey="year" type="number" domain={[firstYear, lastYear]} allowDecimals={false} tick={{ fontSize: 12, fill: '#52514e' }}
        stroke="#bdbdbd" />
      <YAxis domain={[0, yMax]} allowDataOverflow tickFormatter={compactMoney} width={64} tick={{ fontSize: 12, fill: '#52514e' }}
        stroke="#bdbdbd" />
      <RunPaths paths={paths} firstYear={firstYear} lastYear={lastYear} yMax={yMax} />
      {CONFIDENCE_LINES.map(line => (
        <Line key={line.key} dataKey={line.key} name={line.label} stroke={line.color} strokeWidth={2} strokeDasharray={line.dash}
          dot={false} activeDot={false} isAnimationActive={false} />
      ))}
      <InteractionLayer paths={paths} firstYear={firstYear} lastYear={lastYear} yMax={yMax}
        hovered={hovered} selected={selected} onHover={setHoveredIndex} onSelect={onSelectRun} />
      <EndLabels rows={rows} firstYear={firstYear} lastYear={lastYear} yMax={yMax} />
    </LineChart>
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 2, mb: 1 }} data-testid="chart-legend">
        {CONFIDENCE_LINES.map(line => (
          <Box key={line.key} sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <svg width={22} height={8} aria-hidden><line x1={0} x2={22} y1={4} y2={4} stroke={line.color} strokeWidth={2} strokeDasharray={line.dash} /></svg>
            <Typography variant="body2">{line.label}</Typography>
          </Box>
        ))}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
          <svg width={22} height={8} aria-hidden><line x1={0} x2={22} y1={4} y2={4} stroke={COLORS.run} strokeWidth={2} /></svg>
          <Typography variant="body2">Each run</Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
          <svg width={22} height={8} aria-hidden><line x1={0} x2={22} y1={4} y2={4} stroke={COLORS.failedRun} strokeWidth={2} /></svg>
          <Typography variant="body2">Failed run</Typography>
        </Box>
      </Box>
      <Typography variant="body2" color="text.secondary" sx={{ minHeight: 20 }} aria-live="polite">
        {hovered
          ? `Run ${hovered.runIndex + 1}: ending balance ${compactMoney(hovered.endingBalance)}, ` +
            (hovered.isSuccess ? 'reached End Age' : `failed in ${hovered.failureYear}`) +
            ' (click for its summary)'
          : 'Hover over the chart to highlight a run; click to see its summary.'}
      </Typography>
      {width ? chart : <ResponsiveContainer width="100%" height={height}>{chart}</ResponsiveContainer>}
    </Box>
  );
};

export default MonteCarloChart;
