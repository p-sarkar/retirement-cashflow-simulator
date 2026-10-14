import SimulationPage from './pages/SimulationPage';
import './App.css'
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';

const theme = createTheme();

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SimulationPage />
    </ThemeProvider>
  )
}

export default App