import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SaveSimulationDialog from './SaveSimulationDialog';

describe('SaveSimulationDialog', () => {
  it('saves under the name entered when no saved simulation is loaded (FR002-011)', async () => {
    const onSave = vi.fn();
    render(<SaveSimulationDialog open defaultName="Plan" onSave={onSave} onClose={vi.fn()} />);
    const dialog = screen.getByRole('dialog');

    expect(within(dialog).queryByRole('button', { name: /Update/ })).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenCalledWith('Plan');
  });

  it('offers to update the loaded simulation or save a new one (FR005-007)', async () => {
    const onSave = vi.fn();
    const onUpdate = vi.fn();
    render(<SaveSimulationDialog open defaultName="Plan" loadedName="Retire at 65" onUpdate={onUpdate} onSave={onSave} onClose={vi.fn()} />);
    const dialog = screen.getByRole('dialog');

    await userEvent.click(within(dialog).getByRole('button', { name: "Update 'Retire at 65'" }));
    expect(onUpdate).toHaveBeenCalledTimes(1);

    const name = within(dialog).getByLabelText('Name');
    await userEvent.clear(name);
    await userEvent.type(name, 'Retire at 62');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save as new' }));
    expect(onSave).toHaveBeenCalledWith('Retire at 62');
  });

  it('refuses a blank or too long name for a new simulation', async () => {
    render(<SaveSimulationDialog open defaultName="Plan" loadedName="Retire at 65" onUpdate={vi.fn()} onSave={vi.fn()} onClose={vi.fn()} />);
    const dialog = screen.getByRole('dialog');
    const name = within(dialog).getByLabelText('Name');

    await userEvent.clear(name);
    expect(within(dialog).getByRole('button', { name: 'Save as new' })).toBeDisabled();
    await userEvent.type(name, 'x'.repeat(256));
    expect(within(dialog).getByText('Name must be 255 characters or less')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: "Update 'Retire at 65'" })).toBeEnabled(); // keeps its name
  });
});
