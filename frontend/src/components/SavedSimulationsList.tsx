import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { SavedSimulationSummary } from '../types/simulation';
import { formatRunDate } from '../utils/format';
import { simulationNameError } from '../utils/simulationName';

interface SavedSimulationsListProps {
  simulations: SavedSimulationSummary[];
  loading?: boolean;
  error?: string | null;
  loadingId?: string | null; // the simulation being loaded
  onLoad: (id: string) => void;
  onRefresh: () => void;
  onRename: (id: string, name: string) => void;
  onDelete: (id: string) => void;
  onExport?: (id: string) => void; // export its inputs to a file (FR005-013)
}

const formatMoney = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);

/** Rename a saved simulation (FR005-008), with the same name rule as saving. */
const RenameDialog: React.FC<{ simulation: SavedSimulationSummary; onRename: (name: string) => void; onClose: () => void }> = ({
  simulation, onRename, onClose
}) => {
  const [name, setName] = useState(simulation.name);
  const trimmed = name.trim();
  const error = simulationNameError(trimmed);
  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form onSubmit={e => { e.preventDefault(); if (!error) onRename(trimmed); }}>
        <DialogTitle>Rename Simulation</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth margin="dense" label="Name" value={name} onChange={e => setName(e.target.value)}
            error={!!error} helperText={error} />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={!!error}>Rename</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

/**
 * Saved simulations, most recently changed first, with the date each was run or last updated (FR002-012,
 * FR005-011); rename and delete (US005-2).
 */
const SavedSimulationsList: React.FC<SavedSimulationsListProps> = ({
  simulations, loading = false, error, loadingId, onLoad, onRefresh, onRename, onDelete, onExport
}) => {
  const [renaming, setRenaming] = useState<SavedSimulationSummary | null>(null);
  const [deleting, setDeleting] = useState<SavedSimulationSummary | null>(null);
  return (
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
                <TableCell>Last Changed</TableCell>
                <TableCell>Outcome</TableCell>
                <TableCell align="right">Ending Balance</TableCell>
                <TableCell align="right">End Age</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {simulations.map(simulation => (
                <TableRow key={simulation.id} hover>
                  <TableCell>
                    {simulation.name}
                    {/* Results computed by another version may differ from what this version gives (FR005-001) */}
                    {!simulation.isCurrentVersion && (
                      <Chip size="small" variant="outlined" color="warning" sx={{ ml: 1 }}
                        label={simulation.version ? `Computed by v${simulation.version}` : 'Computed by an unknown version'} />
                    )}
                  </TableCell>
                  <TableCell>
                    {simulation.updatedAt ? `Updated ${formatRunDate(simulation.updatedAt)}` : formatRunDate(simulation.createdAt)}
                  </TableCell>
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
                    {onExport && (
                      <Button size="small" sx={{ ml: 1 }} onClick={() => onExport(simulation.id)} aria-label={`Export ${simulation.name}`}>
                        Export
                      </Button>
                    )}
                    <Button size="small" sx={{ ml: 1 }} onClick={() => setRenaming(simulation)} aria-label={`Rename ${simulation.name}`}>
                      Rename
                    </Button>
                    <Button size="small" color="error" onClick={() => setDeleting(simulation)} aria-label={`Delete ${simulation.name}`}>
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {renaming && (
        <RenameDialog
          simulation={renaming}
          onRename={name => { onRename(renaming.id, name); setRenaming(null); }}
          onClose={() => setRenaming(null)}
        />
      )}

      {/* Delete is permanent, so it always asks first (FR005-009) */}
      {deleting && (
        <Dialog open onClose={() => setDeleting(null)} maxWidth="xs">
          <DialogTitle>Delete Simulation</DialogTitle>
          <DialogContent>
            <DialogContentText>{`Delete '${deleting.name}'? This can't be undone.`}</DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleting(null)}>Cancel</Button>
            <Button color="error" variant="contained" onClick={() => { onDelete(deleting.id); setDeleting(null); }}>Delete</Button>
          </DialogActions>
        </Dialog>
      )}
    </Paper>
  );
};

export default SavedSimulationsList;
