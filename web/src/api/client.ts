export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export type Health = { status: string }

// The frontend calls only our own backend, so only `/api/...` paths are allowed.
export async function apiGet<T>(path: string): Promise<T> {
  if (!path.startsWith('/api/')) {
    throw new Error(`Only /api/ paths are allowed: ${path}`)
  }
  // Absolute URL so Node's fetch (tests) resolves it the same way as the browser.
  const url = new URL(path, window.location.origin)
  // Check again after normalization: '/api/../health' resolves to '/health'.
  if (
    url.origin !== window.location.origin ||
    !url.pathname.startsWith('/api/')
  ) {
    throw new Error(`Only /api/ paths are allowed: ${path}`)
  }
  const response = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!response.ok) {
    throw new ApiError(
      response.status,
      `GET ${path} failed: ${response.status}`,
    )
  }
  return (await response.json()) as T
}

export function getHealth(): Promise<Health> {
  return apiGet<Health>('/api/health')
}
