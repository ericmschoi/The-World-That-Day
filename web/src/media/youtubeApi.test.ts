// Loading the YouTube IFrame API script (plan M3b3c T1 (3)). jsdom never
// fetches the script: the fake `YT` and its ready call stand in for it.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  installYouTubeFake,
  youtubeScripts,
  type YouTubeFake,
} from '../test/youtubeFake'

let fake: YouTubeFake | null = null
// A fresh module for every test, so the "loaded once" state never leaks.
let loadYouTubeApi: () => Promise<unknown>

beforeEach(async () => {
  vi.resetModules()
  ;({ loadYouTubeApi } = await import('./youtubeApi'))
})

afterEach(() => {
  // Takes the script tags and the ready callback away as well.
  ;(fake ?? installYouTubeFake()).uninstall()
  fake = null
})

describe('loadYouTubeApi', () => {
  it('uses window.YT when it is already there, with no script', async () => {
    fake = installYouTubeFake()
    await expect(loadYouTubeApi()).resolves.toBe(fake.YT)
    expect(youtubeScripts()).toHaveLength(0)
  })

  it('inserts the script once however often it is called', async () => {
    const calls = [loadYouTubeApi(), loadYouTubeApi(), loadYouTubeApi()]
    const scripts = youtubeScripts()
    expect(scripts).toHaveLength(1)
    expect((scripts[0] as HTMLScriptElement).async).toBe(true)
    expect(scripts[0].getAttribute('src')).toBe(
      'https://www.youtube.com/iframe_api',
    )

    fake = installYouTubeFake()
    fake.announce()
    const results = await Promise.all([...calls, loadYouTubeApi()])
    expect(results.every((yt) => yt === fake?.YT)).toBe(true)
    expect(youtubeScripts()).toHaveLength(1)
  })

  it('still calls a ready callback that was there before, once', async () => {
    const before = vi.fn()
    window.onYouTubeIframeAPIReady = before
    const loaded = loadYouTubeApi()
    fake = installYouTubeFake()
    fake.announce()
    await expect(loaded).resolves.toBe(fake.YT)
    expect(before).toHaveBeenCalledTimes(1)
  })

  it('fails when the script fails, and never inserts it again', async () => {
    const first = loadYouTubeApi()
    youtubeScripts()[0].dispatchEvent(new Event('error'))
    await expect(first).rejects.toThrow()

    let again: Promise<unknown> | undefined
    expect(() => {
      again = loadYouTubeApi()
    }).not.toThrow()
    await expect(again).rejects.toThrow()
    expect(youtubeScripts()).toHaveLength(1)
  })

  it('fails when the ready call comes without a player', async () => {
    const loaded = loadYouTubeApi()
    window.onYouTubeIframeAPIReady?.()
    await expect(loaded).rejects.toThrow()
  })

  it('starts each test with no script and no callback', () => {
    expect(youtubeScripts()).toHaveLength(0)
    expect(window.onYouTubeIframeAPIReady).toBeUndefined()
    expect(window.YT).toBeUndefined()
  })
})
