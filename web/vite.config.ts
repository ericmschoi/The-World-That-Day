import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Fixed so the dev origin stays registrable with the map SDK later (M4).
    port: 5173,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
