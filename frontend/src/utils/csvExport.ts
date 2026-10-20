import { RESULT_COLUMNS, ResultColumn, ResultRow } from './resultColumns';

/** Quote a CSV field if it contains a comma, quote or line break. */
const escapeCsv = (field: string) => (/[",\r\n]/.test(field) ? `"${field.replace(/"/g, '""')}"` : field);

/**
 * The results as CSV, with the same columns as the results table (FR002-010a).
 * Dollar amounts keep cents and no formatting, rates are percentages with two decimals; not-applicable values
 * (shown as ~) are empty.
 */
export function toCsv(rows: ResultRow[], columns: ResultColumn[] = RESULT_COLUMNS): string {
  const header = columns.map(column => escapeCsv(column.csvHeader ?? column.header));
  const lines = rows.map(row =>
    columns.map(column => {
      const value = column.value(row);
      if (value === null) return '';
      if (typeof value === 'number') {
        if (column.money) return value.toFixed(2);
        return column.percent ? (value * 100).toFixed(2) : String(value);
      }
      return escapeCsv(value);
    })
  );
  return [header, ...lines].map(fields => fields.join(',')).join('\r\n') + '\r\n';
}

/** A simulation name as a file name part, e.g. "My Retirement Plan" -> my-retirement-plan. */
export const fileSlug = (simulationName: string | undefined): string =>
  (simulationName ?? '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'simulation';

/** e.g. "My Retirement Plan" yearly -> my-retirement-plan-yearly.csv */
export function csvFileName(simulationName: string | undefined, view: 'yearly' | 'quarterly'): string {
  return `${fileSlug(simulationName)}-${view}.csv`;
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
