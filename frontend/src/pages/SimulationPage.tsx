import React, { useState } from 'react';
import { Container, Typography, Box, CircularProgress, Alert } from '@mui/material';
import SimulationForm from '../components/SimulationForm';
import ResultsTable from '../components/ResultsTable';
import { runSimulation } from '../services/api';
import { SimulationConfig, SimulationResult } from '../types/simulation';

const SimulationPage: React.FC = () => {
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRunSimulation = async (config: SimulationConfig) => {
    setLoading(true);
    setError(null);
    setResult(null);
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
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        Retirement Cash Flow Simulator
      </Typography>
      
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

      {result && <ResultsTable result={result} />}
    </Container>
  );
};

export default SimulationPage;
