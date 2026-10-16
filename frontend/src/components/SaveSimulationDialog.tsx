import React, { useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';

interface SaveSimulationDialogProps {
  open: boolean;
  defaultName: string;
  saving?: boolean;
  onSave: (name: string) => void;
  onClose: () => void;
}

const MAX_NAME_LENGTH = 255;

/**
 * Ask for the name to save a simulation under (FR002-011), starting from the config's name.
 * Mount it when opening, so each save starts from the current default name.
 */
const SaveSimulationDialog: React.FC<SaveSimulationDialogProps> = ({ open, defaultName, saving = false, onSave, onClose }) => {
  const [name, setName] = useState(defaultName);
  const trimmed = name.trim();
  const error = trimmed.length === 0 ? 'Name is required'
    : trimmed.length > MAX_NAME_LENGTH ? `Name must be ${MAX_NAME_LENGTH} characters or less` : undefined;

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
            helperText={error ?? 'Saves the inputs and the results'}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={!!error || saving}>{saving ? 'Saving…' : 'Save'}</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default SaveSimulationDialog;
