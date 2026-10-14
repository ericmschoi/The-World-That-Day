import { apiGet } from '../api/client'
import type { WorldResponse } from './types'

// `GET /api/story/world?date=YYYY-MM-DD` (plan M3b2, contract from M3a2).
// `timeoutMs` defaults to the client's 10 seconds; tests pass a small one.
export function getWorld(
  date: string,
  signal?: AbortSignal,
  timeoutMs?: number,
): Promise<WorldResponse> {
  return apiGet<WorldResponse>(
    `/api/story/world?date=${encodeURIComponent(date)}`,
    { signal, timeoutMs },
  )
}
