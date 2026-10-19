import React, { useCallback, useEffect, useState } from 'react';
import { Container, Typography, Box, CircularProgress, Alert, Chip, Accordion, AccordionSummary, AccordionDetails, Tabs, Tab, Button } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SaveIcon from '@mui/icons-material/Save';
import SimulationForm from '../components/SimulationForm';
import ResultsTable from '../components/ResultsTable';
import SavedSimulationsList from '../components/SavedSimulationsList';
import SaveSimulationDialog from '../components/SaveSimulationDialog';
import MonteCarloResults from '../components/MonteCarloResults';
import RerunNotice, { ResultsShown } from '../components/RerunNotice';
import { InputsFileProblem } from '../components/InputsFileButtons';
import { downloadJson, inputsFileName } from '../utils/inputsFile';
import axios from 'axios';
import {
  deleteSimulation, describeRequestError, exportInputs, getMarketPresets, getMonteCarloRun, isNotFound, listSimulations, loadSimulation,
  MONTE_CARLO_RUNS, renameSimulation, runMonteCarlo, runSimulation, saveSimulation, updateSimulation
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
  // The saved simulation the shown results came from: Save can update it (FR005-007, R005-11)
  const [loaded, setLoaded] = useState<{ id: string; name: string } | null>(null);

  // Saved results computed by another version, and their re-run (US005-1): both kept until the user saves (R005-2)
  const [olderResults, setOlderResults] = useState<{
    savedVersion: string | null;
    runningVersion: string;
    saved: SimulationResult;
    rerun: SimulationResult | null;
    showing: ResultsShown;
  } | null>(null);
  const [rerunning, setRerunning] = useState(false);

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
    setOlderResults(null); // other inputs: the comparison no longer applies (FR005-005)
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
      setLoaded({ id: saved.id, name: saved.name });
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
      setLoaded({ id: saved.summary.id, name: saved.summary.name });
      setOlderResults(saved.summary.isCurrentVersion ? null : {
        savedVersion: saved.summary.version,
        runningVersion: saved.apiMetadata.version,
        saved: saved.result,
        rerun: null,
        showing: 'saved'
      });
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

  // The simulation was deleted elsewhere (FR005-012): the shown results can still be saved as new
  const reportGone = (name: string) => {
    setLoaded(current => (current?.name === name ? null : current));
    setError({ message: `"${name}" no longer exists. Save the results as a new simulation to keep them.`, details: [] });
    refreshSaved();
  };

  // US005-2: replace the loaded simulation's inputs and results with those shown
  const handleUpdate = async () => {
    if (!result || !loaded) return;
    setSaving(true);
    try {
      const updated = await updateSimulation(loaded.id, result);
      setSaveOpen(false);
      setOlderResults(null); // the stored results are now the ones shown (FR005-005)
      setNotice(`Updated "${updated.name}".`);
      refreshSaved();
    } catch (err) {
      setSaveOpen(false);
      if (isNotFound(err)) reportGone(loaded.name);
      else setError(toPageError(err, null, 'update'));
    } finally {
      setSaving(false);
    }
  };

  const handleRename = async (id: string, name: string) => {
    const before = savedSimulations.find(s => s.id === id)?.name ?? name;
    setError(null);
    try {
      const renamed = await renameSimulation(id, name);
      if (loaded?.id === id) setLoaded({ id, name: renamed.name });
      setNotice(`Renamed "${before}" to "${renamed.name}".`);
      refreshSaved();
    } catch (err) {
      if (isNotFound(err)) reportGone(before);
      else setError(toPageError(err, null, 'rename'));
    }
  };

  const handleDelete = async (id: string) => {
    const name = savedSimulations.find(s => s.id === id)?.name ?? 'The simulation';
    setError(null);
    try {
      await deleteSimulation(id);
      // A deleted loaded simulation's results stay shown, unsaved (FR005-010)
      if (loaded?.id === id) setLoaded(null);
      setNotice(`Deleted "${name}".`);
      refreshSaved();
    } catch (err) {
      if (isNotFound(err)) reportGone(name);
      else setError(toPageError(err, null, 'delete'));
    }
  };

  // US005-3: an imported file fills the form; nothing runs until the user does (FR005-014)
  const handleImported = (config: SimulationConfig) => {
    setFormInitialConfig(config);
    setFormKey(key => key + 1);
    setFormExpanded(true);
    setLoaded(null); // the form no longer holds a saved simulation's inputs
    setOlderResults(null);
    setError(null);
    setNotice(`Imported the inputs of "${config.name}". Run them when ready.`);
  };

  const handleInputsFileError = (problem: InputsFileProblem) => {
    setNotice(null);
    setError(problem);
  };

  // US005-3: a saved simulation's inputs, exported from the list (FR005-013)
  const handleExportSaved = async (id: string) => {
    setError(null);
    try {
      const { summary, result: saved } = await loadSimulation(id);
      // Named after the saved simulation, which may have been renamed since its inputs were saved
      downloadJson(inputsFileName(summary.name), await exportInputs(saved.config));
    } catch (err) {
      const name = savedSimulations.find(s => s.id === id)?.name ?? 'The simulation';
      if (isNotFound(err)) reportGone(name);
      else setError(toPageError(err, null, 'export'));
    }
  };

  // US005-1: run the saved inputs with the running version, keeping the saved results to compare (FR005-003)
  const handleRerun = async () => {
    if (!olderResults) return;
    setRerunning(true);
    setError(null);
    try {
      const rerun = await runSimulation(olderResults.saved.config);
      setOlderResults({ ...olderResults, rerun, showing: 'rerun' });
      setResult(rerun);
    } catch (err) {
      setError(toPageError(err, olderResults.saved.config, 're-run'));
    } finally {
      setRerunning(false);
    }
  };

  const handleShowResults = (showing: ResultsShown) => {
    if (!olderResults?.rerun) return;
    setOlderResults({ ...olderResults, showing });
    setResult(showing === 'saved' ? olderResults.saved : olderResults.rerun);
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
            onImportInputs={handleImported}
            onInputsFileError={handleInputsFileError}
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
          {result && olderResults && (
            <RerunNotice
              savedVersion={olderResults.savedVersion}
              runningVersion={olderResults.runningVersion}
              saved={olderResults.saved.summary}
              rerun={olderResults.rerun?.summary ?? null}
              showing={olderResults.showing}
              running={rerunning}
              onRerun={handleRerun}
              onShow={handleShowResults}
            />
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
          onRename={handleRename}
          onDelete={handleDelete}
          onExport={handleExportSaved}
        />
      )}

      {saveOpen && result && (
        <SaveSimulationDialog
          open
          defaultName={result.config.name}
          saving={saving}
          loadedName={loaded?.name}
          onUpdate={handleUpdate}
          onSave={handleSave}
          onClose={() => setSaveOpen(false)}
        />
      )}
    </Container>
  );
};

export default SimulationPage;
