// Tests never reach the real network: any request without an MSW handler fails,
// not only the ones made through our API client (CLAUDE.md 7.5, plan "test policy" 1).
import { afterEach, describe, expect, it, vi } from 'vitest'

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
  })
})
