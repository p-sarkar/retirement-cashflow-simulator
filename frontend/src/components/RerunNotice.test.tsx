import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import RerunNotice from './RerunNotice';
import { Summary } from '../types/simulation';

const saved: Summary = { finalTotalBalance: 1900000, isSuccess: true, failureYear: null, totalDividends: 1, totalInterest: 1 };
const rerun: Summary = { finalTotalBalance: 1500000, isSuccess: false, failureYear: 2049, totalDividends: 1, totalInterest: 1 };

describe('RerunNotice (US005-1)', () => {
  it('names both versions and offers a re-run (FR005-002, FR005-003)', () => {
    const onRerun = vi.fn();
    render(<RerunNotice savedVersion="1.1.28" runningVersion="1.1.29" saved={saved} rerun={null} showing="saved"
      onRerun={onRerun} onShow={vi.fn()} />);

    expect(screen.getByText(/computed by v1\.1\.28/)).toBeInTheDocument();
    expect(screen.getByText(/v1\.1\.29, which may give different results/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Re-run with v1.1.29' }));
    expect(onRerun).toHaveBeenCalledTimes(1);
  });

  it('says when the version is unknown, and disables the re-run while it runs', () => {
    render(<RerunNotice savedVersion={null} runningVersion="1.1.29" saved={saved} rerun={null} showing="saved" running
      onRerun={vi.fn()} onShow={vi.fn()} />);

    expect(screen.getByText(/computed by an unknown version/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Re-running…' })).toBeDisabled();
  });

  it('compares the saved and re-run outcome, failure year and ending balance, and switches between them (FR005-004)', () => {
    const onShow = vi.fn();
    render(<RerunNotice savedVersion="1.1.28" runningVersion="1.1.29" saved={saved} rerun={rerun} showing="rerun"
      onRerun={vi.fn()} onShow={onShow} />);
    const table = screen.getByRole('table', { name: 'Saved and re-run results' });
    const row = (label: string) => within(table).getByRole('row', { name: new RegExp(`^${label}`) });

    expect(row('Outcome')).toHaveTextContent('Success');
    expect(row('Outcome')).toHaveTextContent('Failed');
    expect(row('Failure year')).toHaveTextContent('2049');
    expect(row('Ending balance')).toHaveTextContent('$1,900,000');
    expect(row('Ending balance')).toHaveTextContent('$1,500,000');
    expect(row('Ending balance')).toHaveTextContent('-$400,000');

    expect(screen.getByRole('button', { name: 'Re-run (v1.1.29)' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'Saved (v1.1.28)' }));
    expect(onShow).toHaveBeenCalledWith('saved');
  });
});
