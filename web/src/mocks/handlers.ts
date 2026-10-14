import { http, HttpResponse } from 'msw'
import {
  world1988WithMedia,
  world1995,
  world2002WithMedia,
} from './fixtures/world/responses'

// The world response for each demo date (product brief 12), or undefined.
function worldFor(date: string | null) {
  switch (date) {
    case '1988-09-17':
      return world1988WithMedia
    case '2002-06-22':
      return world2002WithMedia
    case '1995-03-15':
      return world1995
    default:
      return undefined
  }
}

// Default handlers for our own backend only (`/api/...`).
// Tests override them per case with `server.use(...)`.
export const handlers = [
  http.get('/api/health', () => HttpResponse.json({ status: 'UP' })),
  // Any other date is a 500 without a body.
  http.get('/api/story/world', ({ request }) => {
    const body = worldFor(new URL(request.url).searchParams.get('date'))
    return body
      ? HttpResponse.json(body)
      : new HttpResponse(null, { status: 500 })
  }),
]
