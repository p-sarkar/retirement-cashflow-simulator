import React from 'react';
import { Box } from '@mui/material';
import { Layer, Rectangle, Sankey, Tooltip } from 'recharts';
import { Flow } from '../../types/simulation';
import { toSankeyData } from '../../utils/sankeyData';
import { formatMoney } from './format';

interface NodeProps {
  x: number;
  y: number;
  width: number;
  height: number;
  payload: { name: string; value: number };
}

/** A node with its name and the year's amount beside it. */
const LabelledNode = ({ x, y, width, height, payload }: NodeProps) => (
  <Layer>
    <Rectangle x={x} y={y} width={width} height={Math.max(height, 1)} fill="#1976d2" fillOpacity={0.9} />
    <text x={x + width + 6} y={y + height / 2 - 2} fontSize={12} fill="#333">{payload.name}</text>
    <text x={x + width + 6} y={y + height / 2 + 12} fontSize={11} fill="#666">{formatMoney(payload.value)}</text>
  </Layer>
);

/** The year's money flows as a Sankey diagram: where money came from and where it went (FR004-055). */
const MoneyFlowSankey: React.FC<{ flows: Flow[] }> = ({ flows }) => {
  const data = toSankeyData(flows);
  if (data.links.length === 0) return null;
  return (
    <Box sx={{ overflowX: 'auto' }} role="img" aria-label="Money flows for the year">
      <Sankey
        width={1000}
        height={380}
        data={data}
        nodePadding={28}
        nodeWidth={10}
        margin={{ top: 10, right: 130, bottom: 10, left: 10 }}
        link={{ stroke: '#90caf9', strokeOpacity: 0.5 }}
        node={(props: NodeProps) => <LabelledNode {...props} />}
      >
        <Tooltip formatter={(value?: number) => formatMoney(value ?? 0)} />
      </Sankey>
    </Box>
  );
};

export default MoneyFlowSankey;
