import React, { useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';
import { simulationNameError } from '../utils/simulationName';

interface SaveSimulationDialogProps {
  open: boolean;
  defaultName: string;
  saving?: boolean;
  loadedName?: string; // the saved simulation the results came from, which can be updated (FR005-007)
  onUpdate?: () => void;
  onSave: (name: string) => void;
  onClose: () => void;
}

/**
 * Ask for the name to save a simulation under (FR002-011), starting from the config's name; for results from a
 * loaded saved simulation, also offer to update it (FR005-007). Mount it when opening, so each save starts from the
 * current default name.
 */
const SaveSimulationDialog: React.FC<SaveSimulationDialogProps> = ({
  open, defaultName, saving = false, loadedName, onUpdate, onSave, onClose
}) => {
  const [name, setName] = useState(defaultName);
  const trimmed = name.trim();
  const error = simulationNameError(trimmed);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!error && !saving) onSave(trimmed);
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <form onSubmit={handleSubmit}>
        <DialogTitle>Save Simulation</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            margin="dense"
            label="Name"
            value={name}
            onChange={e => setName(e.target.value)}
            error={!!error}
            helperText={error ?? (loadedName ? 'For a new simulation; an update keeps its name' : 'Saves the inputs and the results')}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          {loadedName && onUpdate ? (
            <>
              <Button type="submit" disabled={!!error || saving}>Save as new</Button>
              <Button variant="contained" onClick={onUpdate} disabled={saving}>{`Update '${loadedName}'`}</Button>
            </>
          ) : (
            <Button type="submit" variant="contained" disabled={!!error || saving}>{saving ? 'Saving…' : 'Save'}</Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default SaveSimulationDialog;
