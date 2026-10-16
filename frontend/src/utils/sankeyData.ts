import { Flow } from '../types/simulation';
import { ENDPOINT_LABELS } from '../components/breakdown/format';

export interface SankeyData {
  nodes: { name: string }[];
  links: { source: number; target: number; value: number }[];
}

/** Recharts Sankey input from the year's flows: one node per endpoint, in order of first use (FR004-055). */
export function toSankeyData(flows: Flow[]): SankeyData {
  const index = new Map<string, number>();
  const nodes: { name: string }[] = [];
  const node = (endpoint: Flow['from']) => {
    if (!index.has(endpoint)) {
      index.set(endpoint, nodes.length);
      nodes.push({ name: ENDPOINT_LABELS[endpoint] });
    }
    return index.get(endpoint)!;
  };
  const links = flows.map(f => ({ source: node(f.from), target: node(f.to), value: f.amount }));
  return { nodes, links };
}
