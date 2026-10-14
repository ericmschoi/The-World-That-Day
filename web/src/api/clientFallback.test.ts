// Review fix for plan M3b2 T1: without `AbortSignal.any` (Safari/iOS before
// 17.4) the client still tells a timeout from a caller abort.
import { delay, http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { server } from '../mocks/server'
import { ApiTimeoutError, apiGet } from './client'

const original = Object.getOwnPropertyDescriptor(AbortSignal, 'any')

beforeEach(() => {
  Object.defineProperty(AbortSignal, 'any', {
    value: undefined,
    configurable: true,
    writable: true,
  })
})

afterEach(() => {
  if (original) Object.defineProperty(AbortSignal, 'any', original)
})

// Resolves when the handler has received the request, then never answers.
function hangingHandler() {
  let reached!: () => void
  const received = new Promise<void>((resolve) => {
    reached = resolve
  })
  server.use(
    http.get('/api/slow', async () => {
      reached()
      await delay('infinite')
      return HttpResponse.json({})
    }),
  )
  return received
}

describe('apiGet without AbortSignal.any', () => {
  it('is really running without AbortSignal.any', () => {
    expect(AbortSignal.any).toBeUndefined()
  })

  it('rejects with ApiTimeoutError when the caller signal is still open', async () => {
    void hangingHandler()
    const controller = new AbortController()

    const error = await apiGet('/api/slow?date=1988-09-17', {
      signal: controller.signal,
      timeoutMs: 20,
    }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiTimeoutError)
    expect((error as Error).message).not.toContain('1988-09-17')
  })

  it('rejects with the original AbortError when the caller aborts', async () => {
    const received = hangingHandler()
    const controller = new AbortController()

    const pending = apiGet('/api/slow', { signal: controller.signal }).catch(
      (e: unknown) => e,
    )
    await received
    controller.abort()
    const error = await pending

    expect(error).not.toBeInstanceOf(ApiTimeoutError)
    expect((error as Error).name).toBe('AbortError')
  })

  it('rejects with AbortError when the caller signal is already aborted', async () => {
    void hangingHandler()
    const controller = new AbortController()
    controller.abort()

    const error = await apiGet('/api/slow', {
      signal: controller.signal,
    }).catch((e: unknown) => e)

    expect((error as Error).name).toBe('AbortError')
  })

  it('answers normally and removes its listeners afterwards', async () => {
    const controller = new AbortController()
    const removed: string[] = []
    const remove = controller.signal.removeEventListener.bind(controller.signal)
    controller.signal.removeEventListener = (
      type: string,
      ...rest: [EventListenerOrEventListenerObject]
    ) => {
      removed.push(type)
      remove(type, ...rest)
    }

    await expect(
      apiGet('/api/health', { signal: controller.signal }),
    ).resolves.toEqual({ status: 'UP' })
    expect(removed).toEqual(['abort'])
  })
})
