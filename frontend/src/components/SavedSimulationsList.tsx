import React from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { SavedSimulationSummary } from '../types/simulation';
import { formatRunDate } from '../utils/format';

interface SavedSimulationsListProps {
  simulations: SavedSimulationSummary[];
  loading?: boolean;
  error?: string | null;
  loadingId?: string | null; // the simulation being loaded
  onLoad: (id: string) => void;
  onRefresh: () => void;
}

const formatMoney = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);

/** Saved simulations, newest first, with the date each was run (spec 002 FR-012). */
const SavedSimulationsList: React.FC<SavedSimulationsListProps> = ({
  simulations, loading = false, error, loadingId, onLoad, onRefresh
}) => (
  <Paper sx={{ p: 2, mt: 3 }}>
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
      <Typography variant="h5">Saved Simulations</Typography>
      <Button size="small" startIcon={<RefreshIcon />} onClick={onRefresh} disabled={loading}>Refresh</Button>
    </Box>

    {error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}

    {loading ? (
      <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }} role="status"><CircularProgress /></Box>
    ) : simulations.length === 0 ? (
      !error && (
        <Typography color="text.secondary" sx={{ my: 2 }}>
          No saved simulations yet. Run a simulation, then use Save on the results.
        </Typography>
      )
    ) : (
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Date Run</TableCell>
              <TableCell>Outcome</TableCell>
              <TableCell align="right">Ending Balance</TableCell>
              <TableCell align="right">End Age</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {simulations.map(simulation => (
              <TableRow key={simulation.id} hover>
                <TableCell>{simulation.name}</TableCell>
                <TableCell>{formatRunDate(simulation.createdAt)}</TableCell>
                <TableCell>
                  {simulation.isSuccess
                    ? <Chip size="small" color="success" label="Success" />
                    : <Chip size="small" color="error" label={`Failed in ${simulation.failureYear}`} />}
                </TableCell>
                <TableCell align="right">{formatMoney(simulation.finalTotalBalance)}</TableCell>
                <TableCell align="right">{simulation.endAge}</TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={() => onLoad(simulation.id)}
                    disabled={!!loadingId}
                    aria-label={`Load ${simulation.name}`}
                  >
                    {loadingId === simulation.id ? 'Loading…' : 'Load'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    )}
  </Paper>
);

export default SavedSimulationsList;
