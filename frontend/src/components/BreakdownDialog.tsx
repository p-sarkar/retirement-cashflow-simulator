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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  CircularProgress,
  Alert
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Draggable from 'react-draggable';
import { ComputationBreakdown, SimulationConfig } from '../types/simulation';
import { getBreakdown } from '../services/api';

interface BreakdownDialogProps {
  open: boolean;
  onClose: () => void;
  config: SimulationConfig | null;
  targetAge: number;
}

// Draggable Paper component for the dialog
function PaperComponent(props: any) {
  return (
    <Draggable
      handle="#draggable-dialog-title"
      cancel={'[class*="MuiDialogContent-root"]'}
    >
      <Paper {...props} />
    </Draggable>
  );
}

const formatMoney = (amount: number) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(amount);
};

const formatNumber = (num: number) => {
  if (Math.abs(num) < 1) {
    return num.toFixed(4);
  }
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(num);
};

const BreakdownDialog: React.FC<BreakdownDialogProps> = ({ open, onClose, config, targetAge }) => {
  const [breakdown, setBreakdown] = useState<ComputationBreakdown | null>(null);
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
      PaperComponent={PaperComponent}
      aria-labelledby="draggable-dialog-title"
      PaperProps={{ sx: { minHeight: '80vh' } }}
    >
      <DialogTitle id="draggable-dialog-title" sx={{ cursor: 'move' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
          <Typography variant="h5">
            Computation Breakdown
          </Typography>
          {breakdown && (
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              <Chip label={`Year: ${breakdown.year}`} color="primary" />
              <Chip label={`Age: ${breakdown.age}`} color="secondary" />
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
              This breakdown shows all computations involved in calculating the values for age {breakdown.age} (year {breakdown.year}).
              Each section can be expanded to see the detailed formulas and intermediate values.
            </Typography>

            {breakdown.sections.map((section, sectionIndex) => (
              <Accordion key={sectionIndex} defaultExpanded={false}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="h6">{section.title}</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <TableContainer component={Paper} variant="outlined" sx={{ overflowX: 'auto' }}>
                    <Table size="small" sx={{ tableLayout: 'auto' }}>
                      <TableHead>
                        <TableRow sx={{ backgroundColor: 'grey.100' }}>
                          <TableCell sx={{ fontWeight: 'bold', minWidth: '160px', maxWidth: '180px' }}>Label</TableCell>
                          <TableCell sx={{ fontWeight: 'bold', minWidth: '100px', maxWidth: '130px' }}>Formula</TableCell>
                          <TableCell sx={{ fontWeight: 'bold', minWidth: '280px', maxWidth: '320px' }}>Values</TableCell>
                          <TableCell sx={{ fontWeight: 'bold', minWidth: '110px', maxWidth: '140px' }}>Result</TableCell>
                          <TableCell sx={{ fontWeight: 'bold', minWidth: '80px', maxWidth: '110px' }}>Explanation</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {section.steps.map((step, stepIndex) => (
                          <TableRow
                            key={stepIndex}
                            sx={{
                              '&:nth-of-type(odd)': { backgroundColor: 'grey.50' },
                              '&:hover': { backgroundColor: 'action.hover' }
                            }}
                          >
                            <TableCell sx={{ wordBreak: 'break-word' }}>
                              <Typography variant="body2" fontWeight="medium">
                                {step.label}
                              </Typography>
                            </TableCell>
                            <TableCell sx={{ wordBreak: 'break-word' }}>
                              <Typography
                                variant="body2"
                                sx={{
                                  fontFamily: 'monospace',
                                  backgroundColor: 'grey.200',
                                  px: 0.5,
                                  py: 0.5,
                                  borderRadius: 1,
                                  display: 'inline-block',
                                  fontSize: '0.75rem',
                                  wordBreak: 'break-word',
                                  whiteSpace: 'normal'
                                }}
                              >
                                {step.formula}
                              </Typography>
                            </TableCell>
                            <TableCell sx={{ wordBreak: 'break-word' }}>
                              {Object.keys(step.values).length > 0 ? (
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                                  {Object.entries(step.values).map(([key, value]) => (
                                    <Typography key={key} variant="caption" sx={{ fontFamily: 'monospace', fontSize: '0.7rem' }}>
                                      {key}: {typeof value === 'number' && Math.abs(value) >= 1000
                                        ? formatMoney(value)
                                        : formatNumber(value)}
                                    </Typography>
                                  ))}
                                </Box>
                              ) : (
                                <Typography variant="caption" color="text.secondary">-</Typography>
                              )}
                            </TableCell>
                            <TableCell sx={{ wordBreak: 'break-word' }}>
                              <Typography
                                variant="body2"
                                fontWeight="bold"
                                color={step.result < 0 ? 'error.main' : 'success.main'}
                                sx={{ fontSize: '0.875rem' }}
                              >
                                {Math.abs(step.result) >= 1000
                                  ? formatMoney(step.result)
                                  : formatNumber(step.result)}
                              </Typography>
                            </TableCell>
                            <TableCell sx={{ wordBreak: 'break-word' }}>
                              <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.75rem' }}>
                                {step.explanation}
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </AccordionDetails>
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

