import { apiGet } from '../api/client'

// `GET /api/story/world?date=YYYY-MM-DD` (plan M3b2, contract from M3a2).
// `timeoutMs` defaults to the client's 10 seconds; tests pass a small one.
// Returns `unknown` until T1b adds the `WorldResponse` type.
export function getWorld(
  date: string,
  signal?: AbortSignal,
  timeoutMs?: number,
): Promise<unknown> {
  return apiGet<unknown>(`/api/story/world?date=${encodeURIComponent(date)}`, {
    signal,
    timeoutMs,
  })
}
