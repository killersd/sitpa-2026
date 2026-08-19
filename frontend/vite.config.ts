import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Em desenvolvimento o frontend fala com o backend pelo proxy, entao o navegador enxerga
    // tudo na mesma origem e nao ha CORS no caminho.
    proxy: {
      '/api': {
        target: process.env.SITPA_API_URL ?? 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
