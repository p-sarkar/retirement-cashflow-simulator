import { expect, test, type Page } from '@playwright/test';

// Spec 006 quickstart §5 in the running app: choosing Risk-Based Guardrails, validation, results, breakdown
// and Monte Carlo. Needs the frontend, BFF and API server running.

const choose = async (page: Page, label: string, option: string) => {
  await page.getByRole('combobox', { name: label }).click();
  await page.getByRole('option', { name: option, exact: true }).click();
};

const withdrawalStrategy = (page: Page) => page.getByText('Withdrawal Strategy', { exact: true }).locator('..');

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('combobox', { name: 'Spending Strategy' })).toBeVisible();
});

test('the existing strategy is the default and guardrail settings appear only for guardrails (US006-1)', async ({ page }) => {
  const section = withdrawalStrategy(page);
  await expect(page.getByRole('combobox', { name: 'Spending Strategy' })).toHaveText(/Partha's Spending Strat/);
  await expect(section.getByLabel('Target Success (%)')).toHaveCount(0);

  await choose(page, 'Spending Strategy', 'Risk-Based Guardrails');
  await expect(section.getByLabel('Target Success (%)')).toHaveValue('90');
  await expect(section.getByLabel('Upper Guardrail (%)')).toHaveValue('99');
  await expect(section.getByLabel('Lower Guardrail (%)')).toHaveValue('70');
  await expect(section.getByLabel('TDA Withdrawal % of Needs')).toBeVisible();
});

test('invalid guardrails are rejected on the field (FR006-010)', async ({ page }) => {
  await choose(page, 'Spending Strategy', 'Risk-Based Guardrails');
  await withdrawalStrategy(page).getByLabel('Lower Guardrail (%)').fill('95');
  await page.getByRole('button', { name: 'Run Simulation' }).click();

  await expect(page.getByText('The simulation was not run because some inputs are invalid:')).toBeVisible();
  await expect(withdrawalStrategy(page).getByText('Lower guardrail (95%) must be below the target (90%)')).toBeVisible();
});

test('a guardrails run under the 2008 Crash shows the summary, columns and breakdown (US006-2–US006-4)', async ({ page }) => {
  await choose(page, 'Spending Strategy', 'Risk-Based Guardrails');
  await choose(page, 'Market Conditions', '2008 Crash');
  await page.getByRole('button', { name: 'Run Simulation' }).click();

  const summary = page.getByRole('heading', { name: 'Risk-Based Guardrails' }).locator('..');
  await expect(summary).toBeVisible({ timeout: 30_000 });
  await expect(summary.getByText('Starting probability of success')).toBeVisible();
  await expect(summary.getByText(/^If your portfolio falls below \$[\d,]+, Wants drop to/)).toBeVisible();
  await expect(summary.getByText('Wants per year')).toBeVisible();
  await expect(summary.getByText('Probability of success at each review')).toBeVisible();

  for (const header of ['Wants (configured)', 'Success Probability', 'Guardrail']) {
    await expect(page.getByRole('columnheader', { name: header, exact: true })).toBeVisible();
  }
  // The form starts in the current year, so find the cut by its outcome rather than by year: the 2008 Crash
  // in the first retirement year brings a cut at the next review
  const cutRow = page.getByRole('row').filter({ has: page.getByRole('cell', { name: 'Cut', exact: true }) }).first();
  await expect(cutRow).toBeVisible();

  await cutRow.getByRole('button').first().click();
  const dialog = page.getByRole('dialog');
  await dialog.getByText('Guardrails', { exact: true }).click();
  await expect(dialog.getByText(/below the 70% lower guardrail, so Wants were cut from/)).toBeVisible();
});

test('a guardrails Monte Carlo shows spending statistics (FR006-021)', async ({ page }) => {
  await choose(page, 'Spending Strategy', 'Risk-Based Guardrails');
  await page.getByRole('button', { name: /Run Monte Carlo/ }).click();

  await expect(page.getByText('Runs with a spending cut')).toBeVisible({ timeout: 60_000 });
  await expect(page.getByRole('table', { name: 'Annual spending by year' })).toContainText('Median');
});
