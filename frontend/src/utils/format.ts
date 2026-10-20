import { ApiMetadata } from '../types/simulation';

/** When a saved simulation was run, e.g. "Oct 15, 2026, 10:49 AM". */
export const formatRunDate = (createdAt: string) =>
  new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(createdAt));

/** "Build: 1.1.29+16.gbff4a24, 2026-10-20 09:37:00", or the build time alone for results from before builds were recorded. */
export const buildLabel = (metadata: ApiMetadata) =>
  metadata.build ? `Build: ${metadata.build}, ${metadata.buildTime}` : `Build: ${metadata.buildTime}`;
