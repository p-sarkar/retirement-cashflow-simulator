import { expect, test, type Page } from '@playwright/test';

// FR002-003: a preset sets the market rates, which stay disabled (greyed out) until Custom is chosen.

const choose = async (page: Page, label: string, option: string) => {
  await page.getByRole('combobox', { name: label }).click();
  await page.getByRole('option', { name: option, exact: true }).click();
};

test('a preset locks its rates until Custom is chosen', async ({ page }) => {
  await page.goto('/');
  const rates = page.getByText('Economic Assumptions (%)', { exact: true }).locator('..');
  const inflation = rates.getByLabel('Inflation (%)');

  await choose(page, 'Market Conditions', 'Bull Market');
  await expect(inflation).toHaveValue('2.5');
  await expect(inflation).toBeDisabled();
  await expect(rates.getByText('Set by the Bull Market preset. Choose Custom to edit them.')).toBeVisible();
  await expect(rates.getByLabel('HYSA Rate (%)')).toBeEnabled();

  await choose(page, 'Market Conditions', 'Custom');
  await inflation.fill('3.5');
  await expect(inflation).toHaveValue('3.5');
  await expect(page.getByRole('combobox', { name: 'Market Conditions' })).toHaveText('Custom');
});
