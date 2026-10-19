import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SavedSimulationsList from './SavedSimulationsList';
import { SavedSimulationSummary } from '../types/simulation';

const simulations: SavedSimulationSummary[] = [
  { id: 'b', name: 'Retire at 62', createdAt: '2026-10-15T09:30:00-07:00', isSuccess: false, failureYear: 2049, finalTotalBalance: 0, endAge: 90,
    updatedAt: null, version: '1.1.29', isCurrentVersion: true },
  { id: 'a', name: 'Retire at 65', createdAt: '2026-10-01T14:05:00-07:00', isSuccess: true, failureYear: null, finalTotalBalance: 2500000, endAge: 85,
    updatedAt: null, version: '1.1.29', isCurrentVersion: true }
];

const handlers = () => ({ onLoad: vi.fn(), onRefresh: vi.fn(), onRename: vi.fn(), onDelete: vi.fn() });

describe('SavedSimulationsList (FR002-012, FR002-013)', () => {
  it('lists each saved simulation with its name, date run and outcome', () => {
    render(<SavedSimulationsList simulations={simulations} {...handlers()} />);
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
    render(<SavedSimulationsList simulations={simulations} {...handlers()} onLoad={onLoad} />);
    fireEvent.click(screen.getByRole('button', { name: 'Load Retire at 65' }));
    expect(onLoad).toHaveBeenCalledWith('a');
  });

  it('disables loading while one is in progress', () => {
    render(<SavedSimulationsList simulations={simulations} loadingId="a" {...handlers()} />);
    expect(screen.getByRole('button', { name: 'Load Retire at 65' })).toHaveTextContent('Loading…');
    expect(screen.getByRole('button', { name: 'Load Retire at 62' })).toBeDisabled();
  });

  it('explains an empty list and shows errors', () => {
    const { rerender } = render(<SavedSimulationsList simulations={[]} {...handlers()} />);
    expect(screen.getByText(/No saved simulations yet/)).toBeInTheDocument();

    rerender(<SavedSimulationsList simulations={[]} error="Could not reach the simulation service" {...handlers()} />);
    expect(screen.getByText('Could not reach the simulation service')).toBeInTheDocument();
    expect(screen.queryByText(/No saved simulations yet/)).not.toBeInTheDocument();
  });

  it('marks simulations computed by another or an unknown version (FR005-001, FR005-006)', () => {
    const older = [
      { ...simulations[0], version: '1.1.28', isCurrentVersion: false },
      { ...simulations[1], version: null, isCurrentVersion: false }
    ];
    const { rerender } = render(<SavedSimulationsList simulations={older} {...handlers()} />);
    const rows = screen.getAllByRole('row').slice(1);
    expect(within(rows[0]).getByText('Computed by v1.1.28')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Computed by an unknown version')).toBeInTheDocument();

    rerender(<SavedSimulationsList simulations={simulations} {...handlers()} />);
    expect(screen.queryByText(/Computed by/)).not.toBeInTheDocument();
  });

  it('shows when a simulation was last updated (FR005-011)', () => {
    const updated = [{ ...simulations[1], updatedAt: '2026-10-18T10:00:00-07:00' }];
    render(<SavedSimulationsList simulations={updated} {...handlers()} />);
    expect(screen.getAllByRole('row')[1]).toHaveTextContent(/Updated Oct 18, 2026/);
  });

  it('renames with a trimmed, non-blank name (FR005-008)', async () => {
    const h = handlers();
    render(<SavedSimulationsList simulations={simulations} {...h} />);
    await userEvent.click(screen.getByRole('button', { name: 'Rename Retire at 65' }));
    const dialog = screen.getByRole('dialog');
    const name = within(dialog).getByLabelText('Name');
    expect(name).toHaveValue('Retire at 65');

    await userEvent.clear(name);
    expect(within(dialog).getByRole('button', { name: 'Rename' })).toBeDisabled();
    await userEvent.type(name, '  Retire at 66  ');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(h.onRename).toHaveBeenCalledWith('a', 'Retire at 66');
  });

  it('deletes only after a confirmation that names the simulation (FR005-009)', async () => {
    const h = handlers();
    render(<SavedSimulationsList simulations={simulations} {...h} />);

    await userEvent.click(screen.getByRole('button', { name: 'Delete Retire at 65' }));
    expect(screen.getByRole('dialog')).toHaveTextContent("Delete 'Retire at 65'? This can't be undone.");
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }));
    expect(h.onDelete).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Delete Retire at 65' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' }));
    expect(h.onDelete).toHaveBeenCalledWith('a');
  });
});
