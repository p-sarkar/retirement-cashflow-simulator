import React, { useCallback, useEffect, useState } from 'react';
import { Container, Typography, Box, CircularProgress, Alert, Chip, Accordion, AccordionSummary, AccordionDetails, Tabs, Tab, Button } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SaveIcon from '@mui/icons-material/Save';
import SimulationForm from '../components/SimulationForm';
import ResultsTable from '../components/ResultsTable';
import SavedSimulationsList from '../components/SavedSimulationsList';
import SaveSimulationDialog from '../components/SaveSimulationDialog';
import MonteCarloResults from '../components/MonteCarloResults';
import axios from 'axios';
import {
  describeRequestError, getMarketPresets, getMonteCarloRun, listSimulations, loadSimulation, MONTE_CARLO_RUNS,
  runMonteCarlo, runSimulation, saveSimulation
} from '../services/api';
import {
  MarketPreset, MonteCarloResult, MonteCarloRunDetail, SavedSimulationSummary, SimulationConfig, SimulationResult,
  ValidationErrorResponse
} from '../types/simulation';
import { formatValidationErrors } from '../utils/expenseUtils';
import { formatRunDate } from '../utils/format';

type View = 'results' | 'monte-carlo' | 'saved';

interface PageError {
  message: string;
  details: string[];
}

/** A failed request as a message, listing per-field problems for a 422. */
function toPageError(err: unknown, config: SimulationConfig | null, action: string): PageError {
  console.error(err);
  if (axios.isAxiosError<ValidationErrorResponse>(err) && err.response?.status === 422 && err.response.data?.validationErrors) {
    return {
      message: `The ${action} was not run because some inputs are invalid:`,
      details: formatValidationErrors(err.response.data.validationErrors, config?.oneTimeExpenses ?? [])
    };
  }
  return { message: describeRequestError(err), details: [] };
}

const SimulationPage: React.FC = () => {
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [currentConfig, setCurrentConfig] = useState<SimulationConfig | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<PageError | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [formExpanded, setFormExpanded] = useState(true);
  const [view, setView] = useState<View>('results');
  const [presets, setPresets] = useState<MarketPreset[]>([]);

  // Loading a saved simulation remounts the form with its config
  const [formKey, setFormKey] = useState(0);
  const [formInitialConfig, setFormInitialConfig] = useState<SimulationConfig | undefined>(undefined);

  // Saved simulations (US002-2)
  const [saveOpen, setSaveOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedSimulations, setSavedSimulations] = useState<SavedSimulationSummary[]>([]);
  const [savedLoading, setSavedLoading] = useState(false);
  const [savedError, setSavedError] = useState<string | null>(null);
  const [loadingSavedId, setLoadingSavedId] = useState<string | null>(null);

  // Monte Carlo (US002-3, US002-4)
  const [monteCarlo, setMonteCarlo] = useState<MonteCarloResult | null>(null);
  const [monteCarloConfig, setMonteCarloConfig] = useState<SimulationConfig | null>(null);
  const [monteCarloLoading, setMonteCarloLoading] = useState(false);
  const [selectedRun, setSelectedRun] = useState<number | null>(null);
  const [runDetail, setRunDetail] = useState<MonteCarloRunDetail | null>(null);
  const [runDetailLoading, setRunDetailLoading] = useState(false);
  const [runDetailError, setRunDetailError] = useState<string | null>(null);

  const refreshSaved = useCallback(async () => {
    setSavedLoading(true);
    setSavedError(null);
    try {
      setSavedSimulations(await listSimulations());
    } catch (err) {
      setSavedError(describeRequestError(err));
    } finally {
      setSavedLoading(false);
    }
  }, []);

  useEffect(() => {
    // Without presets the form simply offers custom market conditions
    getMarketPresets().then(setPresets).catch(err => console.error('Could not load market presets', err));
    refreshSaved();
  }, [refreshSaved]);

  const handleRunSimulation = async (config: SimulationConfig) => {
    setLoading(true);
    setError(null);
    setNotice(null);
    setResult(null);
    setCurrentConfig(config);
    setView('results');
    try {
      const data = await runSimulation(config);
      setResult(data);
      // Auto-collapse form after successful simulation
      setFormExpanded(false);
    } catch (err: unknown) {
      setError(toPageError(err, config, 'simulation'));
      // Keep the form open so the inputs can be corrected and re-run
      setFormExpanded(true);
    } finally {
      setLoading(false);
    }
  };

  const handleRunMonteCarlo = async (config: SimulationConfig) => {
    setMonteCarloLoading(true);
    setError(null);
    setNotice(null);
    setMonteCarlo(null);
    setSelectedRun(null);
    setRunDetail(null);
    setRunDetailError(null);
    setMonteCarloConfig(config);
    setView('monte-carlo');
    try {
      setMonteCarlo(await runMonteCarlo(config));
      setFormExpanded(false);
    } catch (err: unknown) {
      setError(toPageError(err, config, 'Monte Carlo analysis'));
      setFormExpanded(true);
    } finally {
      setMonteCarloLoading(false);
    }
  };

  // US002-4: the analysis returns summaries only; the server rebuilds one run in full from the seed
  const handleLoadRunDetails = async (runIndex: number) => {
    if (!monteCarlo || !monteCarloConfig) return;
    setRunDetailLoading(true);
    setRunDetailError(null);
    try {
      setRunDetail(await getMonteCarloRun(monteCarloConfig, monteCarlo.seed, runIndex));
    } catch (err) {
      setRunDetailError(describeRequestError(err));
    } finally {
      setRunDetailLoading(false);
    }
  };

  const handleSave = async (name: string) => {
    if (!result) return;
    setSaving(true);
    try {
      const saved = await saveSimulation(name, result);
      setSaveOpen(false);
      setNotice(`Saved "${saved.name}".`);
      refreshSaved();
    } catch (err) {
      setSaveOpen(false);
      setError(toPageError(err, null, 'save'));
    } finally {
      setSaving(false);
    }
  };

  const handleLoadSaved = async (id: string) => {
    setLoadingSavedId(id);
    setError(null);
    setNotice(null);
    try {
      const saved = await loadSimulation(id);
      const config = saved.result.config;
      setFormInitialConfig(config);
      setFormKey(key => key + 1);
      setCurrentConfig(config);
      setResult(saved.result);
      // A Monte Carlo analysis belongs to the inputs it was run with
      setMonteCarlo(null);
      setSelectedRun(null);
      setRunDetail(null);
      setFormExpanded(false);
      setView('results');
      setNotice(`Loaded "${saved.summary.name}", run ${formatRunDate(saved.summary.createdAt)}. The form holds its inputs.`);
    } catch (err) {
      setError(toPageError(err, null, 'load'));
    } finally {
      setLoadingSavedId(null);
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
          <SimulationForm
            key={formKey}
            initialConfig={formInitialConfig}
            presets={presets}
            onSubmit={handleRunSimulation}
            running={loading}
            onRunMonteCarlo={handleRunMonteCarlo}
            monteCarloRunning={monteCarloLoading}
          />
        </AccordionDetails>
      </Accordion>

      {error && (
        <Alert severity="error" sx={{ mt: 2 }} onClose={() => setError(null)}>
          {error.message}
          {error.details.length > 0 && (
            <Box component="ul" sx={{ m: 0, mt: 0.5, pl: 2.5 }}>
              {error.details.map((detail, i) => <li key={i}>{detail}</li>)}
            </Box>
          )}
        </Alert>
      )}
      {notice && <Alert severity="success" sx={{ mt: 2 }} onClose={() => setNotice(null)}>{notice}</Alert>}

      <Tabs value={view} onChange={(_e, value: View) => setView(value)} sx={{ mt: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab value="results" label="Cash Flow" />
        <Tab value="monte-carlo" label="Monte Carlo" />
        <Tab value="saved" label={`Saved Simulations${savedSimulations.length ? ` (${savedSimulations.length})` : ''}`} />
      </Tabs>

      {view === 'results' && (
        <>
          {loading && (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 2, my: 4 }} role="status">
              <CircularProgress />
              <Typography color="text.secondary">Running simulation…</Typography>
            </Box>
          )}
          {!loading && !result && (
            <Typography color="text.secondary" sx={{ my: 3 }}>Run a simulation to see the year-by-year cash flow.</Typography>
          )}
          {result && (
            <ResultsTable
              result={result}
              config={currentConfig}
              headerActions={
                <Button size="small" variant="outlined" startIcon={<SaveIcon />} onClick={() => setSaveOpen(true)}>Save</Button>
              }
            />
          )}
        </>
      )}

      {view === 'monte-carlo' && (
        <>
          {monteCarloLoading && (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 2, my: 4 }} role="status">
              <CircularProgress />
              <Typography color="text.secondary">Running {MONTE_CARLO_RUNS.toLocaleString()} simulations…</Typography>
            </Box>
          )}
          {!monteCarloLoading && !monteCarlo && (
            <Typography color="text.secondary" sx={{ my: 3 }}>
              Use Run Monte Carlo on the form to simulate {MONTE_CARLO_RUNS.toLocaleString()} sequences of market returns,
              inflation and bond yields with your other inputs.
            </Typography>
          )}
          {monteCarlo && (
            <MonteCarloResults
              result={monteCarlo}
              selectedRunIndex={selectedRun}
              onSelectRun={setSelectedRun}
              detail={runDetail}
              detailLoading={runDetailLoading}
              detailError={runDetailError}
              onLoadDetails={handleLoadRunDetails}
            />
          )}
        </>
      )}

      {view === 'saved' && (
        <SavedSimulationsList
          simulations={savedSimulations}
          loading={savedLoading}
          error={savedError}
          loadingId={loadingSavedId}
          onLoad={handleLoadSaved}
          onRefresh={refreshSaved}
        />
      )}

      {saveOpen && result && (
        <SaveSimulationDialog
          open
          defaultName={result.config.name}
          saving={saving}
          onSave={handleSave}
          onClose={() => setSaveOpen(false)}
        />
      )}
    </Container>
  );
};

export default SimulationPage;
