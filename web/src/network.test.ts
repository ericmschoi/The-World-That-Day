// Tests never reach the real network: any request without an MSW handler fails,
// not only the ones made through our API client (CLAUDE.md 7.5, plan "test policy" 1).
import { afterEach, describe, expect, it, vi } from 'vitest'
import { takeUnhandledRequests } from './test/unhandledRequests'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('network isolation in tests', () => {
  it('fails a direct fetch to an external host instead of sending it', async () => {
    const consoleError = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)

    await expect(fetch('https://example.com/blocked')).rejects.toThrow()
    expect(consoleError).toHaveBeenCalledWith(
      expect.stringMatching(
        /without a matching request handler[\s\S]*example\.com\/blocked/,
      ),
    )
    // Expected here, so take it off the list that fails the test in setup.ts.
    expect(takeUnhandledRequests()).toEqual(['GET https://example.com/blocked'])
  })

  it('records an unhandled request even when its rejection is caught', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined)

    const result = await fetch('/api/__swallowed__').catch(() => 'caught')

    // setup.ts fails the test on any request left in this list.
    expect(result).toBe('caught')
    expect(takeUnhandledRequests()).toEqual([
      `GET ${window.location.origin}/api/__swallowed__`,
    ])
  })
})
