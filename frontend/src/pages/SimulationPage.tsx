import React, { useState } from 'react';
import { Container, Typography, Box, CircularProgress, Alert, Chip, Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SimulationForm from '../components/SimulationForm';
import ResultsTable from '../components/ResultsTable';
import axios from 'axios';
import { runSimulation } from '../services/api';
import { SimulationConfig, SimulationResult, ValidationErrorResponse } from '../types/simulation';
import { formatValidationErrors } from '../utils/expenseUtils';

const SimulationPage: React.FC = () => {
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [currentConfig, setCurrentConfig] = useState<SimulationConfig | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);
  const [formExpanded, setFormExpanded] = useState(true);

  const handleRunSimulation = async (config: SimulationConfig) => {
    setLoading(true);
    setError(null);
    setErrorDetails([]);
    setResult(null);
    setCurrentConfig(config);
    try {
      const data = await runSimulation(config);
      setResult(data);
      // Auto-collapse form after successful simulation
      setFormExpanded(false);
    } catch (err: any) {
      console.error(err);
      if (axios.isAxiosError<ValidationErrorResponse>(err) && err.response?.status === 422 && err.response.data?.validationErrors) {
        setError('The simulation was not run because some inputs are invalid:');
        setErrorDetails(formatValidationErrors(err.response.data.validationErrors, config.oneTimeExpenses));
      } else {
        setError(err.message || 'Failed to run simulation');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth={false} sx={{ mt: 1, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
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

      <Accordion expanded={formExpanded} onChange={() => setFormExpanded(!formExpanded)}>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Simulation Parameters</Typography>
        </AccordionSummary>
        <AccordionDetails sx={{ display: 'flex', justifyContent: 'flex-start', alignItems: 'flex-start', p: 0 }}>
          <SimulationForm onSubmit={handleRunSimulation} />
        </AccordionDetails>
      </Accordion>

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
          {errorDetails.length > 0 && (
            <Box component="ul" sx={{ m: 0, mt: 0.5, pl: 2.5 }}>
              {errorDetails.map((detail, i) => <li key={i}>{detail}</li>)}
            </Box>
          )}
        </Alert>
      )}

      {result && <ResultsTable result={result} config={currentConfig} />}
    </Container>
  );
};

export default SimulationPage;
