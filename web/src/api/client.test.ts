import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../mocks/server'
import { ApiError, apiGet, getHealth } from './client'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('api client', () => {
  it('returns the health status from our backend', async () => {
    await expect(getHealth()).resolves.toEqual({ status: 'UP' })
  })

  it('throws ApiError with the status code on a non-2xx response', async () => {
    server.use(
      http.get('/api/health', () =>
        HttpResponse.json({ status: 'DOWN' }, { status: 503 }),
      ),
    )

    const error = await getHealth().catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(503)
  })

  it('rejects absolute URLs without sending a request', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')

    await expect(apiGet('https://example.com/x')).rejects.toThrow(
      'Only /api/ paths are allowed',
    )
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('rejects paths outside /api/ without sending a request', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')

    await expect(apiGet('/health')).rejects.toThrow(
      'Only /api/ paths are allowed',
    )
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('fails a request that has no MSW handler', async () => {
    // MSW logs the unhandled request; keep test output clean but still check it.
    const consoleError = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)

    await expect(apiGet('/api/__unhandled__')).rejects.toThrow()
    // The message comes from `onUnhandledRequest: 'error'` ('warn' would use console.warn).
    expect(consoleError).toHaveBeenCalledWith(
      expect.stringMatching(
        /without a matching request handler[\s\S]*\/api\/__unhandled__/,
      ),
    )
  })
})
