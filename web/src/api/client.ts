// Error messages name the request path only, never the query string: query
// values (such as a date) must not end up in error messages or logs.
export class ApiError extends Error {
  readonly status: number
  // The server's problem `code` (plan M3a2), or null if the body had none.
  readonly code: string | null

  constructor(status: number, message: string, code: string | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export class ApiTimeoutError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiTimeoutError'
  }
}

export type ApiGetOptions = { signal?: AbortSignal; timeoutMs?: number }

// Product brief §8: a section gives up after 10 seconds.
export const DEFAULT_TIMEOUT_MS = 10_000

const ACCEPT = 'application/json, application/problem+json'

export type Health = { status: string }

function withoutQuery(path: string): string {
  return path.split(/[?#]/, 1)[0]
}

// The frontend calls only our own backend, so only `/api/...` paths are allowed.
// A caller abort rejects with the caller's own error (an `AbortError`), which
// callers ignore; running out of `timeoutMs` rejects with `ApiTimeoutError`.
export async function apiGet<T>(
  path: string,
  options: ApiGetOptions = {},
): Promise<T> {
  if (!path.startsWith('/api/')) {
    throw new Error(`Only /api/ paths are allowed: ${withoutQuery(path)}`)
  }
  // Absolute URL so Node's fetch (tests) resolves it the same way as the browser.
  const url = new URL(path, window.location.origin)
  // Check again after normalization: '/api/../health' resolves to '/health'.
  if (
    url.origin !== window.location.origin ||
    !url.pathname.startsWith('/api/')
  ) {
    throw new Error(`Only /api/ paths are allowed: ${withoutQuery(path)}`)
  }
  const label = `GET ${url.pathname}`
  const { signal: callerSignal, timeoutMs = DEFAULT_TIMEOUT_MS } = options
  const timeout = AbortSignal.timeout(timeoutMs)
  const signal = callerSignal
    ? AbortSignal.any([callerSignal, timeout])
    : timeout

  try {
    const response = await fetch(url, { headers: { Accept: ACCEPT }, signal })
    if (!response.ok) {
      const code = await readProblemCode(response)
      throw new ApiError(
        response.status,
        `${label} failed: ${response.status}`,
        code,
      )
    }
    return (await response.json()) as T
  } catch (error) {
    // The combined signal keeps the reason of whichever signal fired first.
    // An HTTP error stays an ApiError even if time ran out reading its body.
    if (
      !(error instanceof ApiError) &&
      timeout.aborted &&
      signal.reason === timeout.reason
    ) {
      throw new ApiTimeoutError(`${label} timed out after ${timeoutMs} ms`)
    }
    throw error
  }
}

// Reads `code` from a failed response's body only if the body is JSON and
// `code` is a string; anything else (empty body, HTML, no `code`) gives null.
async function readProblemCode(response: Response): Promise<string | null> {
  try {
    const body: unknown = await response.json()
    if (typeof body === 'object' && body !== null && 'code' in body) {
      return typeof body.code === 'string' ? body.code : null
    }
  } catch {
    // Not JSON.
  }
  return null
}

export function getHealth(): Promise<Health> {
  return apiGet<Health>('/api/health')
}
