import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import MonteCarloChart from './MonteCarloChart';
import MonteCarloResults from './MonteCarloResults';
import { monteCarloResult, monteCarloRunDetail } from '../test/fixtures';

// jsdom has no layout: give the chart a fixed size, and the SVG a position for pointer events
const renderChart = (onSelectRun = vi.fn(), selectedRunIndex: number | null = null) => {
  render(<MonteCarloChart result={monteCarloResult()} selectedRunIndex={selectedRunIndex} onSelectRun={onSelectRun} width={600} height={300} />);
  const hitArea = screen.getByTestId('run-hit-area');
  const svg = (hitArea as unknown as SVGRectElement).ownerSVGElement!;
  svg.getBoundingClientRect = () => ({ left: 0, top: 0, right: 600, bottom: 300, width: 600, height: 300, x: 0, y: 0, toJSON: () => ({}) });
  return { hitArea, onSelectRun };
};

// Pointer position for a balance in a year, from the hit area's plot rectangle
const pointAt = (hitArea: HTMLElement, yearIndex: number, balance: number, yMax = 2000000) => {
  const x = Number(hitArea.getAttribute('x')), y = Number(hitArea.getAttribute('y'));
  const width = Number(hitArea.getAttribute('width')), height = Number(hitArea.getAttribute('height'));
  return { clientX: x + (yearIndex / 3) * width, clientY: y + height - (balance / yMax) * height };
};

describe('MonteCarloChart (FR-015 to FR-017)', () => {
  it('plots one line per run and a legend for the confidence lines', () => {
    renderChart();
    expect(screen.getByTestId('run-paths').querySelectorAll('path')).toHaveLength(3);
    const legend = screen.getByTestId('chart-legend');
    ['Median', '75% of runs above', '90% of runs above', 'Each run', 'Failed run'].forEach(label =>
      expect(legend).toHaveTextContent(label));
  });

  it('highlights the run under the pointer and describes it', () => {
    const { hitArea } = renderChart();
    fireEvent.mouseMove(hitArea, pointAt(hitArea, 2, 1400000));

    expect(screen.getByTestId('hovered-run')).toBeInTheDocument();
    expect(screen.getByText(/Run 1: ending balance \$1\.6M, reached End Age/)).toBeInTheDocument();

    fireEvent.mouseLeave(hitArea);
    expect(screen.queryByTestId('hovered-run')).not.toBeInTheDocument();
  });

  it('selects the clicked run', () => {
    const { hitArea, onSelectRun } = renderChart();
    fireEvent.click(hitArea, pointAt(hitArea, 2, 200000));
    expect(onSelectRun).toHaveBeenCalledWith(2);
  });

  it('draws the selected run', () => {
    renderChart(vi.fn(), 1);
    expect(screen.getByTestId('selected-run')).toBeInTheDocument();
  });
});

describe('MonteCarloResults (US3, US4)', () => {
  const props = {
    result: monteCarloResult(),
    onSelectRun: vi.fn(),
    onLoadDetails: vi.fn(),
    detail: null,
    chartWidth: 600
  };

  it('summarises the analysis and lists the confidence lines as a table', () => {
    render(<MonteCarloResults {...props} selectedRunIndex={null} />);
    expect(screen.getByText('66.7%')).toBeInTheDocument();
    expect(screen.getByText('2 of 3 runs reached End Age')).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Confidence lines by year' });
    expect(table).toHaveTextContent('90% of runs above');
    expect(table).toHaveTextContent('$200K');
  });

  it('shows summary metrics for a clicked run, and Load Details asks for its full results', () => {
    const onLoadDetails = vi.fn();
    render(<MonteCarloResults {...props} onLoadDetails={onLoadDetails} selectedRunIndex={2} />);

    const summary = screen.getByTestId('selected-run-summary');
    expect(summary).toHaveTextContent('Run 3');
    expect(summary).toHaveTextContent('Failed in 2027');
    expect(summary).toHaveTextContent('7.1%'); // average market return
    expect(screen.queryByRole('table', { name: 'Market variables by year' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Load Details' }));
    expect(onLoadDetails).toHaveBeenCalledWith(2);
  });

  it('shows the run variables as rows and its cash flow table', () => {
    render(<MonteCarloResults {...props} selectedRunIndex={1} detail={monteCarloRunDetail(1)} />);

    const variables = screen.getByRole('table', { name: 'Market variables by year' });
    const rows = Array.from(variables.querySelectorAll('tbody tr')).map(tr => tr.textContent);
    expect(rows).toEqual(['Market return-12.0%21.5%5.0%', 'Bond yield5.0%3.9%4.1%', 'Inflation3.1%2.0%1.8%']);
    expect(screen.getByText('Run 2: cash flow')).toBeInTheDocument();
    // The breakdown recomputes from static rates, so it is not offered for a Monte Carlo run
    expect(screen.queryByText('Details')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Load Details' })).toBeDisabled();
  });
});
