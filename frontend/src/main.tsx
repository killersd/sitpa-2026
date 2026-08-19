import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import { ProvedorAutenticacao } from './auth/Autenticacao';
import './styles/global.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // O protocolo muda raramente durante um plantao; revalidar a cada foco de janela so
      // geraria trafego. O cadastro invalida as consultas explicitamente quando altera algo.
      refetchOnWindowFocus: false,
      staleTime: 30_000,
      retry: 1,
    },
  },
});

const raiz = document.getElementById('root');
if (!raiz) {
  throw new Error('Elemento #root nao encontrado no index.html.');
}

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ProvedorAutenticacao>
          <App />
        </ProvedorAutenticacao>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
