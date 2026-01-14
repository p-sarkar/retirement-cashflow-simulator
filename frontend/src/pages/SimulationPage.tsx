import React, { useState } from 'react';
import { Container, Typography, Box, CircularProgress, Alert, Chip } from '@mui/material';
import SimulationForm from '../components/SimulationForm';
import ResultsTable from '../components/ResultsTable';
import { runSimulation } from '../services/api';
import { SimulationConfig, SimulationResult } from '../types/simulation';

const SimulationPage: React.FC = () => {
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [currentConfig, setCurrentConfig] = useState<SimulationConfig | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRunSimulation = async (config: SimulationConfig) => {
    setLoading(true);
    setError(null);
    setResult(null);
    setCurrentConfig(config);
    try {
      const data = await runSimulation(config);
      setResult(data);
    } catch (err: any) {
      console.error(err);
      setError(err.message || 'Failed to run simulation');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth={false} sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">
          Retirement Cash Flow Simulator
        </Typography>
        {result && result.apiMetadata && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Chip
              label={`API v${result.apiMetadata.version}`}
              color="primary"
              size="small"
            />
            <Chip
              label={`Build: ${result.apiMetadata.buildTime}`}
              variant="outlined"
              size="small"
            />
            <Chip
              label={`Started: ${result.apiMetadata.serverStartTime}`}
              variant="outlined"
              size="small"
            />
          </Box>
        )}
      </Box>

      <SimulationForm onSubmit={handleRunSimulation} />

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}

      {result && <ResultsTable result={result} config={currentConfig} />}
    </Container>
  );
};

export default SimulationPage;
