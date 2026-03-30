import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import { MatrixSessionProvider } from './matrix/MatrixSessionContext';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MatrixSessionProvider>
      <App />
    </MatrixSessionProvider>
  </StrictMode>,
);
