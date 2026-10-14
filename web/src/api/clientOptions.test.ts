// Timeout, caller abort, Accept header and problem `code` (plan M3b2 T1).
import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../mocks/server'
import { ApiError, ApiTimeoutError, apiGet, DEFAULT_TIMEOUT_MS } from './client'

afterEach(() => {
  vi.restoreAllMocks()
})

// Resolves when the handler has received the request, then never answers.
function hangingHandler(path: string) {
  let reached!: () => void
  const received = new Promise<void>((resolve) => {
    reached = resolve
  })
  server.use(
    http.get(path, async () => {
      reached()
      await delay('infinite')
      return HttpResponse.json({})
    }),
  )
  return received
}

describe('apiGet options', () => {
  it('rejects with ApiTimeoutError when the response is slower than timeoutMs', async () => {
    void hangingHandler('/api/slow')

    const error = await apiGet('/api/slow?date=1988-09-17', {
      timeoutMs: 20,
    }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiTimeoutError)
    expect((error as Error).message).toBe('GET /api/slow timed out after 20 ms')
    expect((error as Error).message).not.toContain('1988-09-17')
  })

  it('times out with the caller signal too, as long as the caller did not abort', async () => {
    void hangingHandler('/api/slow')
    const controller = new AbortController()

    await expect(
      apiGet('/api/slow', { signal: controller.signal, timeoutMs: 20 }),
    ).rejects.toBeInstanceOf(ApiTimeoutError)
  })

  it('rejects with the original AbortError when the caller aborts', async () => {
    const received = hangingHandler('/api/slow')
    const controller = new AbortController()

    const pending = apiGet('/api/slow', { signal: controller.signal }).catch(
      (e: unknown) => e,
    )
    await received
    controller.abort()
    const error = await pending

    expect(error).not.toBeInstanceOf(ApiTimeoutError)
    expect(error).not.toBeInstanceOf(ApiError)
    expect((error as Error).name).toBe('AbortError')
  })

  it('rejects with AbortError without a timeout when the signal is already aborted', async () => {
    void hangingHandler('/api/slow')
    const controller = new AbortController()
    controller.abort()

    const error = await apiGet('/api/slow', {
      signal: controller.signal,
    }).catch((e: unknown) => e)

    expect((error as Error).name).toBe('AbortError')
  })

  it('uses a 10 second timeout by default', async () => {
    const timeoutSpy = vi.spyOn(AbortSignal, 'timeout')

    await apiGet('/api/health')

    expect(DEFAULT_TIMEOUT_MS).toBe(10_000)
    expect(timeoutSpy).toHaveBeenCalledWith(10_000)
  })

  it('accepts both JSON and problem+json', async () => {
    let accept: string | null = null
    server.use(
      http.get('/api/health', ({ request }) => {
        accept = request.headers.get('accept')
        return HttpResponse.json({ status: 'UP' })
      }),
    )

    await apiGet('/api/health')

    expect(accept).toContain('application/json')
    expect(accept).toContain('application/problem+json')
  })
})

describe('apiGet error code', () => {
  it('reads code from a problem+json body', async () => {
    server.use(
      http.get('/api/problem', () =>
        HttpResponse.json(
          { status: 400, code: 'DATE_NOT_EXIST', detail: 'No such date.' },
          {
            status: 400,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    )

    const error = await apiGet('/api/problem').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).code).toBe('DATE_NOT_EXIST')
  })

  it.each([
    [
      'a non-JSON body',
      () => new HttpResponse('<h1>oops</h1>', { status: 500 }),
    ],
    ['an empty body', () => new HttpResponse(null, { status: 500 })],
    ['JSON without code', () => HttpResponse.json({ x: 1 }, { status: 500 })],
    [
      'a non-string code',
      () => HttpResponse.json({ code: 7 }, { status: 500 }),
    ],
    ['a JSON array', () => HttpResponse.json([1], { status: 500 })],
  ])('gives code null for %s', async (_name, respond) => {
    server.use(http.get('/api/problem', respond))

    const error = await apiGet('/api/problem').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(500)
    expect((error as ApiError).code).toBeNull()
  })

  it('keeps the query string out of path check errors', async () => {
    await expect(apiGet('/health?date=1988-09-17')).rejects.toThrow(
      /^Only \/api\/ paths are allowed: \/health$/,
    )
  })
})
