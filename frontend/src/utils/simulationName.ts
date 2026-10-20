/** The longest saved simulation name the API accepts (its name column). */
export const MAX_NAME_LENGTH = 255;

/** Why a (trimmed) name can't be saved, or undefined if it can (FR002-011, FR005-008). */
export const simulationNameError = (trimmed: string): string | undefined =>
  trimmed.length === 0 ? 'Name is required'
    : trimmed.length > MAX_NAME_LENGTH ? `Name must be ${MAX_NAME_LENGTH} characters or less` : undefined;
