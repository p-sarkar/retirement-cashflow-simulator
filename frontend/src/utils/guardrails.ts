import { GuardrailReview } from '../types/simulation';

export const money = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);

/**
 * The first review's guardrails in dollars (US006-3): "If your portfolio falls below $X, Wants drop to $Y a year;
 * above $Z, they rise to $W a year." A guardrail no portfolio reaches is left out; null when neither is known.
 */
export function dollarGuardrailsSentence(review: GuardrailReview | undefined): string | null {
  if (!review) return null;
  const lower = review.lowerGuardrailPortfolio != null && review.wantsAtLowerGuardrail != null
    ? `falls below ${money(review.lowerGuardrailPortfolio)}, Wants drop to ${money(review.wantsAtLowerGuardrail)} a year` : null;
  const upper = review.upperGuardrailPortfolio != null && review.wantsAtUpperGuardrail != null
    ? `${money(review.upperGuardrailPortfolio)}, ${lower ? 'they' : 'Wants'} rise to ${money(review.wantsAtUpperGuardrail)} a year` : null;
  if (lower && upper) return `If your portfolio ${lower}; above ${upper}.`;
  if (lower) return `If your portfolio ${lower}.`;
  if (upper) return `If your portfolio rises above ${upper}.`;
  return null;
}

