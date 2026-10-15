/** When a saved simulation was run, e.g. "Oct 15, 2026, 10:49 AM". */
export const formatRunDate = (createdAt: string) =>
  new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(createdAt));
