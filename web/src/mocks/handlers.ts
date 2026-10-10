import { http, HttpResponse } from 'msw'

// Default handlers for our own backend only (`/api/...`).
// Tests override them per case with `server.use(...)`.
export const handlers = [
  http.get('/api/health', () => HttpResponse.json({ status: 'UP' })),
]
