import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import SavedSimulationsList from './SavedSimulationsList';
import { SavedSimulationSummary } from '../types/simulation';

const simulations: SavedSimulationSummary[] = [
  { id: 'b', name: 'Retire at 62', createdAt: '2026-10-15T09:30:00-07:00', isSuccess: false, failureYear: 2049, finalTotalBalance: 0, endAge: 90 },
  { id: 'a', name: 'Retire at 65', createdAt: '2026-10-01T14:05:00-07:00', isSuccess: true, failureYear: null, finalTotalBalance: 2500000, endAge: 85 }
];

describe('SavedSimulationsList (FR002-012, FR002-013)', () => {
  it('lists each saved simulation with its name, date run and outcome', () => {
    render(<SavedSimulationsList simulations={simulations} onLoad={vi.fn()} onRefresh={vi.fn()} />);
    const rows = screen.getAllByRole('row').slice(1);

    expect(rows).toHaveLength(2);
    expect(within(rows[0]).getByText('Retire at 62')).toBeInTheDocument();
    expect(within(rows[0]).getByText('Failed in 2049')).toBeInTheDocument();
    expect(rows[0]).toHaveTextContent(/Oct 15, 2026/);
    expect(within(rows[1]).getByText('Success')).toBeInTheDocument();
    expect(rows[1]).toHaveTextContent('$2,500,000');
  });

  it('loads the clicked simulation', () => {
    const onLoad = vi.fn();
    render(<SavedSimulationsList simulations={simulations} onLoad={onLoad} onRefresh={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Load Retire at 65' }));
    expect(onLoad).toHaveBeenCalledWith('a');
  });

  it('disables loading while one is in progress', () => {
    render(<SavedSimulationsList simulations={simulations} loadingId="a" onLoad={vi.fn()} onRefresh={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Load Retire at 65' })).toHaveTextContent('Loading…');
    expect(screen.getByRole('button', { name: 'Load Retire at 62' })).toBeDisabled();
  });

  it('explains an empty list and shows errors', () => {
    const { rerender } = render(<SavedSimulationsList simulations={[]} onLoad={vi.fn()} onRefresh={vi.fn()} />);
    expect(screen.getByText(/No saved simulations yet/)).toBeInTheDocument();

    rerender(<SavedSimulationsList simulations={[]} error="Could not reach the simulation service" onLoad={vi.fn()} onRefresh={vi.fn()} />);
    expect(screen.getByText('Could not reach the simulation service')).toBeInTheDocument();
    expect(screen.queryByText(/No saved simulations yet/)).not.toBeInTheDocument();
  });
});
