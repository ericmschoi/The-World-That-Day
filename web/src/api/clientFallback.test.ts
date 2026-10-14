// Review fix for plan M3b2 T1: without `AbortSignal.any` (Safari/iOS before
// 17.4) the client still tells a timeout from a caller abort.
import { delay, http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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
  vi.restoreAllMocks()
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

// M3b2 second review, recommendation 1 (plan M3b2b T4 (4)): the timeout
// signal from `AbortSignal.timeout` must lose its 'abort' listener too, or
// every finished request leaves a listener behind until the timer fires.
type Listener = EventListenerOrEventListenerObject

// Records the 'abort' listeners added to and removed from `signal`.
function watchAbortListeners(signal: AbortSignal) {
  const added: Listener[] = []
  const removed: Listener[] = []
  const add = signal.addEventListener.bind(signal)
  const remove = signal.removeEventListener.bind(signal)
  signal.addEventListener = (type: string, listener: Listener) => {
    if (type === 'abort') added.push(listener)
    add(type, listener)
  }
  signal.removeEventListener = (type: string, listener: Listener) => {
    if (type === 'abort') removed.push(listener)
    remove(type, listener)
  }
  return {
    added,
    removed,
    // Listeners added and never removed.
    left: () => added.filter((listener) => !removed.includes(listener)),
  }
}

// Wraps every timeout signal the client creates, keeping the real timer.
function watchTimeoutSignals() {
  const realTimeout = AbortSignal.timeout.bind(AbortSignal)
  const watched: ReturnType<typeof watchAbortListeners>[] = []
  vi.spyOn(AbortSignal, 'timeout').mockImplementation((ms: number) => {
    const signal = realTimeout(ms)
    watched.push(watchAbortListeners(signal))
    return signal
  })
  return watched
}

describe('apiGet without AbortSignal.any: timeout signal listeners', () => {
  it('removes the timeout signal’s abort listener exactly once after a normal answer', async () => {
    const timeouts = watchTimeoutSignals()
    const controller = new AbortController()

    await expect(
      apiGet('/api/health', { signal: controller.signal }),
    ).resolves.toEqual({ status: 'UP' })

    expect(timeouts).toHaveLength(1)
    const [timeout] = timeouts
    expect(timeout.added).toHaveLength(1)
    expect(timeout.removed).toEqual(timeout.added)
    expect(timeout.left()).toEqual([])
  })

  it('removes the timeout signal’s abort listener after the caller aborts', async () => {
    const timeouts = watchTimeoutSignals()
    const received = hangingHandler()
    const controller = new AbortController()

    const pending = apiGet('/api/slow', { signal: controller.signal }).catch(
      (e: unknown) => e,
    )
    await received
    controller.abort()
    const error = await pending

    expect((error as Error).name).toBe('AbortError')
    expect(timeouts).toHaveLength(1)
    const [timeout] = timeouts
    expect(timeout.added).toHaveLength(1)
    expect(timeout.removed.length).toBeGreaterThanOrEqual(1)
    expect(timeout.left()).toEqual([])
  })

  it('removes the timeout signal’s abort listener after the time runs out', async () => {
    const timeouts = watchTimeoutSignals()
    void hangingHandler()
    const controller = new AbortController()
    const caller = watchAbortListeners(controller.signal)

    const error = await apiGet('/api/slow', {
      signal: controller.signal,
      timeoutMs: 20,
    }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiTimeoutError)
    expect(timeouts).toHaveLength(1)
    const [timeout] = timeouts
    expect(timeout.added).toHaveLength(1)
    expect(timeout.removed.length).toBeGreaterThanOrEqual(1)
    expect(timeout.left()).toEqual([])
    // The caller's signal is let go as well.
    expect(caller.left()).toEqual([])
  })
})
