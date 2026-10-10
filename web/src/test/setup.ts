import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from '../mocks/server'
import {
  recordUnhandledRequest,
  takeUnhandledRequests,
} from './unhandledRequests'

// Any request without a matching handler is rejected (no real network)...
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
// ...and recorded, because code that catches the rejection would hide it.
server.events.on('request:unhandled', ({ request }) => {
  recordUnhandledRequest(request)
})
afterEach(() => {
  const unhandled = takeUnhandledRequests()
  cleanup()
  server.resetHandlers()
  if (unhandled.length > 0) {
    throw new Error(
      `Requests without an MSW handler:\n${unhandled.map((r) => `  ${r}`).join('\n')}`,
    )
  }
})
afterAll(() => server.close())
