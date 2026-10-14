import react from '@vitejs/plugin-react'
import { configDefaults, defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Fixed so the dev origin stays registrable with the map SDK later (M4).
    port: 5173,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // e2e/ is Playwright's (npm run e2e), not Vitest's.
    exclude: [...configDefaults.exclude, 'e2e/**'],
    // Vitest blanks every CSS import by default, `?raw` included. Let `?raw`
    // reads through so tests can check the real text of tokens.css and global.css.
    css: { include: [/\.css\?raw$/] },
  },
})
