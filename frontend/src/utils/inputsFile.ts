import { fileSlug } from './csvExport';

/** The largest file accepted as an inputs file (FR005-016); the BFF enforces the same limit. */
export const MAX_IMPORT_BYTES = 1024 * 1024;

/** e.g. "My Retirement Plan" -> my-retirement-plan-inputs.json (FR005-013) */
export const inputsFileName = (simulationName: string | undefined) => `${fileSlug(simulationName)}-inputs.json`;

/** Have the browser save [data] as a JSON file. */
export function downloadJson(fileName: string, data: unknown) {
  const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

/** A file's text (FileReader works everywhere, including test environments without File.text()). */
const readText = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file);
  });

/**
 * A chosen file's JSON content, or why it can't be an inputs file. Only the size and JSON are checked here, so the
 * request isn't sent for nothing; the API server decides whether it is an inputs file and whether its inputs are valid.
 */
export async function readInputsFile(file: File): Promise<{ content: unknown } | { problem: string }> {
  if (file.size > MAX_IMPORT_BYTES) return { problem: 'This file is too large to be a simulation inputs file (over 1 MB).' };
  try {
    return { content: JSON.parse(await readText(file)) };
  } catch {
    return { problem: "This file isn't a simulation inputs file." };
  }
}
