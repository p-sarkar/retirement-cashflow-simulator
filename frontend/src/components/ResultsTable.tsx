import React, { useState } from 'react';
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableContainer, 
  TableHead, 
  TableRow, 
  Paper,
  Typography,
  Alert,
  ToggleButton,
  ToggleButtonGroup,
  Box,
  IconButton,
  Tooltip,
  Button
} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import DownloadIcon from '@mui/icons-material/Download';
import { SimulationResult, SimulationConfig } from '../types/simulation';
import BreakdownDialog from './BreakdownDialog';
import { RESULT_COLUMNS, ResultColumn, ResultRow } from '../utils/resultColumns';
import { csvFileName, downloadCsv, toCsv } from '../utils/csvExport';

interface ResultsTableProps {
  result: SimulationResult | null;
  config: SimulationConfig | null;
  title?: string;
  // The breakdown recomputes the row from the config's static rates, so it does not apply to Monte Carlo runs
  showBreakdown?: boolean;
  headerActions?: React.ReactNode; // e.g. a Save button
}

const formatMoney = (amount: number) => {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);
};

// Income Gap and SB Cap are null in working years (salary covers expenses), shown as ~
const formatCell = (column: ResultColumn, row: ResultRow) => {
  const value = column.value(row);
  if (value === null) return '~';
  return column.money && typeof value === 'number' ? formatMoney(value) : value;
};

const subSx = { fontSize: '0.75rem', color: 'text.secondary' };
const headerSx = (column: ResultColumn) => ({
  ...(column.minWidth && { minWidth: column.minWidth }),
  ...((column.emphasis === 'header' || column.emphasis === 'both') && { fontWeight: 'bold' }),
  ...(column.sub && subSx)
});
const cellSx = (column: ResultColumn) => ({
  ...((column.emphasis === 'cell' || column.emphasis === 'both') && { fontWeight: 'bold' }),
  ...(column.sub && subSx)
});

const ResultsTable: React.FC<ResultsTableProps> = ({
  result, config, title = 'Simulation Results', showBreakdown = true, headerActions
}) => {
  const [viewMode, setViewMode] = useState<'yearly' | 'quarterly'>('yearly');
  const [breakdownOpen, setBreakdownOpen] = useState(false);
  const [selectedAge, setSelectedAge] = useState<number>(0);
  const [selectedQuarter, setSelectedQuarter] = useState<number | undefined>(undefined);

  if (!result) return null;

  const { yearlyResults, quarterlyResults, summary } = result;

  const handleViewChange = (
    _event: React.MouseEvent<HTMLElement>,
    newView: 'yearly' | 'quarterly' | null,
  ) => {
    if (newView !== null) {
      setViewMode(newView);
    }
  };

  // Quarterly rows open their year's breakdown on that quarter (FR004-008); quarters are 0-based in results
  const handleBreakdownClick = (row: ResultRow) => {
    setSelectedAge(row.age);
    setSelectedQuarter('quarter' in row ? row.quarter + 1 : undefined);
    setBreakdownOpen(true);
  };


  const rows: ResultRow[] = viewMode === 'yearly' ? yearlyResults : (quarterlyResults || []);

  const handleExport = () => downloadCsv(csvFileName(result.config.name, viewMode), toCsv(rows));

  return (
    <Paper sx={{ p: 2, mt: 3, width: '100%', overflowX: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">{title}</Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {headerActions}
          <Button size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport}>
            Export CSV
          </Button>
          <ToggleButtonGroup
            value={viewMode}
            exclusive
            onChange={handleViewChange}
            aria-label="view mode"
            size="small"
          >
            <ToggleButton value="yearly" aria-label="yearly">
              Yearly
            </ToggleButton>
            <ToggleButton value="quarterly" aria-label="quarterly">
              Quarterly
            </ToggleButton>
          </ToggleButtonGroup>
        </Box>
      </Box>
      
      {summary.isSuccess ? (
        <Alert severity="success" sx={{ mb: 2 }}>
          Success! Plan survived to age {yearlyResults[yearlyResults.length - 1]?.age} with ending balance of {formatMoney(summary.finalTotalBalance)}.
        </Alert>
      ) : (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failure! Plan failed at year {summary.failureYear}.
        </Alert>
      )}

      <TableContainer sx={{ maxHeight: 1200 }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              {showBreakdown && <TableCell>Details</TableCell>}
              {RESULT_COLUMNS.map(column => (
                <TableCell key={column.header} sx={headerSx(column)}>{column.header}</TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, index) => {
                const isFailure = row.metrics.isFailure;
                const isAge75 = row.age === 75;

                // Determine background color: failure (red) > age 75 (yellow) > default
                const backgroundColor = isFailure ? '#ffebee' : (isAge75 ? '#fff9c4' : 'inherit');

                return (
                  <TableRow key={index} sx={{ backgroundColor }}>
                    {showBreakdown && (
                      <TableCell>
                        <Tooltip title="View detailed computation breakdown">
                          <IconButton
                            size="small"
                            onClick={() => handleBreakdownClick(row)}
                            color="primary"
                          >
                            <InfoIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    )}
                    {RESULT_COLUMNS.map(column => (
                      <TableCell key={column.header} sx={cellSx(column)}>{formatCell(column, row)}</TableCell>
                    ))}
                  </TableRow>
                );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      {showBreakdown && (
        <BreakdownDialog
          open={breakdownOpen}
          onClose={() => setBreakdownOpen(false)}
          config={config}
          targetAge={selectedAge}
          initialQuarter={selectedQuarter}
        />
      )}
    </Paper>
  );
};

export default ResultsTable;
