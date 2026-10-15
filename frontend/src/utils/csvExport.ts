import { RESULT_COLUMNS, ResultRow } from './resultColumns';

/** Quote a CSV field if it contains a comma, quote or line break. */
const escapeCsv = (field: string) => (/[",\r\n]/.test(field) ? `"${field.replace(/"/g, '""')}"` : field);

/**
 * The results as CSV, with the same columns as the results table (spec 002 FR-010a).
 * Dollar amounts keep cents and no formatting; not-applicable values (shown as ~) are empty.
 */
export function toCsv(rows: ResultRow[]): string {
  const header = RESULT_COLUMNS.map(column => escapeCsv(column.csvHeader ?? column.header));
  const lines = rows.map(row =>
    RESULT_COLUMNS.map(column => {
      const value = column.value(row);
      if (value === null) return '';
      if (typeof value === 'number') return column.money ? value.toFixed(2) : String(value);
      return escapeCsv(value);
    })
  );
  return [header, ...lines].map(fields => fields.join(',')).join('\r\n') + '\r\n';
}

/** e.g. "My Retirement Plan" yearly -> my-retirement-plan-yearly.csv */
export function csvFileName(simulationName: string | undefined, view: 'yearly' | 'quarterly'): string {
  const slug = (simulationName ?? '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'simulation';
  return `${slug}-${view}.csv`;
}

/** Have the browser save [content] as a CSV file. */
export function downloadCsv(fileName: string, content: string) {
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
