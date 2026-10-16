import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip,
  CircularProgress,
  Alert
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { SimulationConfig, YearBreakdown } from '../types/simulation';
import { getBreakdown } from '../services/api';
import StartingPositionSection from './breakdown/StartingPositionSection';
import MoneyMovementSection from './breakdown/MoneyMovementSection';
import ExpensesSection from './breakdown/ExpensesSection';
import IncomeSection from './breakdown/IncomeSection';
import IncomeGapSection from './breakdown/IncomeGapSection';
import TaxesSection from './breakdown/TaxesSection';
import NextYearSection from './breakdown/NextYearSection';

interface BreakdownDialogProps {
  open: boolean;
  onClose: () => void;
  config: SimulationConfig | null;
  targetAge: number;
  initialQuarter?: number; // 1–4: opened from a quarterly row, so open Money Movement on that quarter (FR004-008)
}

const PHASES = { START: 'Start year', WORKING: 'Working year', RETIREMENT: 'Retirement year' } as const;

/** The seven sections of spec 004, in order; each renders only when the server filled it. */
const sections = (b: YearBreakdown, quarter?: number): { title: string; content: React.ReactNode | null; open?: boolean }[] => [
  { title: 'Starting Position', content: <StartingPositionSection position={b.startingPosition} />, open: !quarter },
  { title: 'Expenses', content: b.expenses && <ExpensesSection section={b.expenses} /> },
  { title: 'Income', content: b.income && <IncomeSection section={b.income} /> },
  { title: 'Income Gap', content: b.incomeGap && <IncomeGapSection section={b.incomeGap} /> },
  {
    title: 'Money Movement',
    content: b.moneyMovement && <MoneyMovementSection section={b.moneyMovement} highlightQuarter={quarter} />,
    open: !!quarter
  },
  { title: 'Taxes', content: b.taxes && <TaxesSection section={b.taxes} /> },
  {
    title: 'Next Year',
    content: b.phase !== 'START' && <NextYearSection section={b.nextYear} isLastYear={b.isLastYear} />
  }
];

const BreakdownDialog: React.FC<BreakdownDialogProps> = ({ open, onClose, config, targetAge, initialQuarter }) => {
  const [breakdown, setBreakdown] = useState<YearBreakdown | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open && config && targetAge) {
      setLoading(true);
      setError(null);
      getBreakdown(config, targetAge)
        .then(data => {
          setBreakdown(data);
          setLoading(false);
        })
        .catch(err => {
          setError(err.message || 'Failed to load breakdown');
          setLoading(false);
        });
    }
  }, [open, config, targetAge]);

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{ sx: { minHeight: '80vh' } }}
    >
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
          <Typography variant="h5" component="span">
            Computation Breakdown
          </Typography>
          {breakdown && (
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              <Chip label={`Year: ${breakdown.year}`} color="primary" />
              <Chip label={`Age: ${breakdown.age}`} color="secondary" />
              <Chip label={PHASES[breakdown.phase]} variant="outlined" />
              {breakdown.apiMetadata && (
                <>
                  <Chip label={`API v${breakdown.apiMetadata.version}`} size="small" variant="outlined" />
                  <Chip label={`Build: ${breakdown.apiMetadata.buildTime}`} size="small" variant="outlined" />
                </>
              )}
            </Box>
          )}
        </Box>
      </DialogTitle>

      <DialogContent dividers>
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 200 }}>
            <CircularProgress />
          </Box>
        )}

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {breakdown && !loading && (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              How the simulation computed age {breakdown.age} (year {breakdown.year}), in the order the year happens.
            </Typography>

            {sections(breakdown, initialQuarter).filter(s => s.content).map(s => (
              <Accordion key={s.title} defaultExpanded={s.open}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="h6" component="span">{s.title}</Typography>
                </AccordionSummary>
                <AccordionDetails>{s.content}</AccordionDetails>
              </Accordion>
            ))}

          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} variant="contained">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default BreakdownDialog;
