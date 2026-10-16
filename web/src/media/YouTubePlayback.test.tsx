// Plan M3b3c T3: YouTube playback rules with the shared coordinator. Fake
// IntersectionObserver, media element and `YT`; no network.
import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ConsentContext } from '../consent/consent'
import { videoFile } from '../mocks/fixtures/media/modes'
import {
  exampleYoutubeEmbed,
  exampleYoutubeUndated,
} from '../mocks/fixtures/world/youtube'
import {
  installIntersectionObserverFake,
  installMediaElementFake,
  type IntersectionObserverFake,
  type MediaElementFake,
} from '../test/mediaFakes'
import {
  type FakePlayer,
  installYouTubeFake,
  PlayerState,
  type YouTubeFake,
} from '../test/youtubeFake'
import { MediaItem } from './MediaItem'
import { PlaybackProvider } from './PlaybackProvider'
import type { Media } from './types'

const { PLAYING, PAUSED, ENDED } = PlayerState

let observers: IntersectionObserverFake
let media: MediaElementFake
let fake: YouTubeFake

beforeEach(() => {
  observers = installIntersectionObserverFake()
  media = installMediaElementFake()
  fake = installYouTubeFake()
})

afterEach(() => {
  observers.uninstall()
  media.uninstall()
  fake.uninstall()
  Reflect.deleteProperty(window, 'matchMedia')
  Reflect.deleteProperty(navigator, 'connection')
})

const agreed = { agreed: true, setAgreed: () => {} }

// One result screen: consent given, one coordinator for every card.
async function mount(items: Media[], { ready = true } = {}) {
  render(
    <ConsentContext value={agreed}>
      <PlaybackProvider>
        {items.map((item) => (
          <MediaItem key={item.id} media={item} fallbackAlt="기록 영상" />
        ))}
      </PlaybackProvider>
    </ConsentContext>,
  )
  await act(async () => {})
  if (ready) for (const player of fake.players) fake.ready(player)
}

const iframes = () => [...document.querySelectorAll('iframe')]
const videos = () =>
  [...document.querySelectorAll('video')] as HTMLVideoElement[]
const playerOf = (iframe: Element) => fake.playersFor(iframe)[0]

// The playback calls our code made on a player, in order.
const PLAYBACK = ['mute', 'playVideo', 'pauseVideo']
const log = (player: FakePlayer) =>
  fake.callsOf(player).filter((method) => PLAYBACK.includes(method))
const count = (player: FakePlayer, method: string) =>
  log(player).filter((m) => m === method).length

// Players playing now: YouTube by its reported state, videos by `paused`.
const playingNow = () =>
  fake.players.filter((p) => p.state === PLAYING).length +
  videos().filter((video) => !video.paused).length

describe('autoplay past half visible (T3 (1)·(2))', () => {
  it('mutes, then plays, once ready and past half', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    observers.setRatio(iframe, 0.51)
    expect(log(playerOf(iframe))).toEqual(['mute', 'playVideo'])
  })

  it('plays once it is ready when it was already past half', async () => {
    await mount([exampleYoutubeEmbed], { ready: false })
    const [iframe] = iframes()
    observers.setRatio(iframe, 0.51)
    const player = playerOf(iframe)
    expect(log(player)).toEqual([])
    fake.ready(player)
    expect(log(player)).toEqual(['mute', 'playVideo'])
  })

  it('pauses at half once it reported playing', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    observers.setRatio(iframe, 0.51)
    expect(playerOf(iframe).state).toBe(PLAYING)
    observers.setRatio(iframe, 0.5)
    expect(log(playerOf(iframe))).toEqual(['mute', 'playVideo', 'pauseVideo'])
  })

  it('plays once for two "in view" reports in a row', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    observers.setRatio(iframe, 0.51)
    observers.setRatio(iframe, 0.8)
    expect(count(playerOf(iframe), 'playVideo')).toBe(1)
  })

  it('leaves another playing player alone on repeated "out of view"', async () => {
    await mount([videoFile, exampleYoutubeEmbed])
    const [iframe] = iframes()
    const [video] = videos()
    observers.setRatio(video, 0.51)
    expect(video.paused).toBe(false)
    observers.setRatio(iframe, 0.3)
    observers.setRatio(iframe, 0)
    observers.setRatio(iframe, 0.2)
    expect(media.pause).not.toHaveBeenCalled()
    expect(log(playerOf(iframe))).toEqual([])
  })
})

describe('a video the user starts under half visible (T3 (3)·(4))', () => {
  it.each([
    ['muted', true],
    ['sound on', false],
  ])(
    'takes over, pauses off screen and autoplays again (%s)',
    async (_c, muted) => {
      await mount([videoFile, exampleYoutubeEmbed])
      const [iframe] = iframes()
      const [video] = videos()
      const player = playerOf(iframe)
      player.muted = muted
      observers.setRatio(video, 0.51)
      observers.setRatio(iframe, 0.3)
      fake.state(player, PLAYING)
      // The coordinator paused the video for it.
      expect(media.pause.mock.contexts).toEqual([video])
      observers.setRatio(iframe, 0)
      expect(count(player, 'pauseVideo')).toBe(1)
      // Our pause, not the user's: it autoplays when it comes back.
      observers.setRatio(iframe, 0.51)
      expect(count(player, 'playVideo')).toBe(1)
      if (!muted) expect(count(player, 'mute')).toBe(0)
    },
  )
})

describe('the user pauses it (T3 (5)·(9))', () => {
  it('does not autoplay again until the user plays it', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    const player = playerOf(iframe)
    observers.setRatio(iframe, 0.51)
    fake.state(player, PAUSED)
    observers.setRatio(iframe, 0.5)
    observers.setRatio(iframe, 0.51)
    expect(count(player, 'playVideo')).toBe(1)

    fake.state(player, PLAYING)
    observers.setRatio(iframe, 0.5)
    expect(count(player, 'pauseVideo')).toBe(1)
    observers.setRatio(iframe, 0.51)
    expect(count(player, 'playVideo')).toBe(2)
  })

  it('does not autoplay again after it ended', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    const player = playerOf(iframe)
    observers.setRatio(iframe, 0.51)
    fake.state(player, ENDED)
    observers.setRatio(iframe, 0.5)
    observers.setRatio(iframe, 0.51)
    expect(count(player, 'playVideo')).toBe(1)
  })
})

describe('late events (T3 (6)·(7))', () => {
  it('counts our own pauses: pause, play, pause before any event', async () => {
    await mount([videoFile, exampleYoutubeEmbed])
    const [iframe] = iframes()
    const [video] = videos()
    const player = playerOf(iframe)
    observers.setRatio(iframe, 0.51)
    expect(player.state).toBe(PLAYING)

    fake.deferEvents()
    observers.setRatio(iframe, 0.5)
    observers.setRatio(iframe, 0.51)
    observers.setRatio(iframe, 0.5)
    // The video plays while the YouTube events are still on their way.
    observers.setRatio(video, 0.51)
    fake.flushEvents()
    // The stale PLAYING did not take over from the video.
    expect(media.pause).not.toHaveBeenCalled()
    expect(video.paused).toBe(false)
    // None of it read as the user's pause.
    observers.setRatio(iframe, 0.51)
    expect(count(player, 'playVideo')).toBe(3)
  })

  it.each(['none', 'paused'] as const)(
    'stops a start it cancelled before the report (pause before start: %s)',
    async (mode) => {
      await mount([videoFile, exampleYoutubeEmbed])
      const [iframe] = iframes()
      const [video] = videos()
      const player = playerOf(iframe)
      fake.setPauseBeforeStart(mode)
      fake.deferEvents()
      observers.setRatio(iframe, 0.51)
      observers.setRatio(iframe, 0.5)
      expect(log(player)).toEqual(['mute', 'playVideo', 'pauseVideo'])
      observers.setRatio(video, 0.51)
      fake.flushEvents()
      // A late PLAYING (mode none) is paused once more and not announced.
      expect(count(player, 'pauseVideo')).toBe(mode === 'none' ? 2 : 1)
      expect(player.state).toBe(PAUSED)
      expect(media.pause).not.toHaveBeenCalled()
      // Not the user's pause: it autoplays when it comes back.
      observers.setRatio(iframe, 0.51)
      expect(count(player, 'playVideo')).toBe(2)
    },
  )
})

describe('one at a time (T3 (8))', () => {
  it('two YouTube players past half together: one plays', async () => {
    await mount([exampleYoutubeEmbed, exampleYoutubeUndated])
    for (const iframe of iframes()) observers.setRatio(iframe, 0.51)
    expect(playingNow()).toBe(1)
    expect(playerOf(iframes()[1]).state).toBe(PLAYING)
  })

  it('YouTube and a video, alternating three times: never two', async () => {
    await mount([videoFile, exampleYoutubeEmbed])
    const [iframe] = iframes()
    const [video] = videos()
    for (let i = 0; i < 3; i += 1) {
      observers.setRatio(video, 0.51)
      expect(playingNow()).toBe(1)
      observers.setRatio(iframe, 0.51)
      expect(playingNow()).toBe(1)
      expect(video.paused).toBe(true)
      observers.setRatio(video, 0.3)
      observers.setRatio(iframe, 0.3)
    }
  })
})

describe('no autoplay when the reader asks for less (T3 (10))', () => {
  it.each([
    [
      'reduced motion',
      () =>
        Object.defineProperty(window, 'matchMedia', {
          configurable: true,
          value: (query: string) => ({
            matches: query === '(prefers-reduced-motion: reduce)',
          }),
        }),
    ],
    [
      'data saving',
      () =>
        Object.defineProperty(navigator, 'connection', {
          configurable: true,
          value: { saveData: true },
        }),
    ],
  ])(
    '%s: no mute or play, but a user play still takes over',
    async (_c, set) => {
      set()
      await mount([videoFile, exampleYoutubeEmbed])
      const [iframe] = iframes()
      const [video] = videos()
      const player = playerOf(iframe)
      observers.setRatio(iframe, 0.51)
      expect(log(player)).toEqual([])
      // The user plays the video file, then the YouTube one.
      await act(() => video.play())
      fake.state(player, PLAYING)
      expect(video.paused).toBe(true)
    },
  )
})

describe('blocked autoplay (T3 (11))', () => {
  it('keeps the card, with no error text', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    observers.setRatio(iframe, 0.51)
    fake.autoplayBlocked(playerOf(iframe))
    expect(iframes()).toEqual([iframe])
    expect(screen.getByRole('figure')).toContainElement(iframe)
    expect(document.body).not.toHaveTextContent(/오류|실패|재생할 수 없/)
  })

  it('takes a later tap as the user’s, not as our cancelled start', async () => {
    await mount([exampleYoutubeEmbed])
    const [iframe] = iframes()
    const player = playerOf(iframe)
    fake.deferEvents()
    observers.setRatio(iframe, 0.51)
    // Blocked: the start never comes, only the blocked report.
    fake.discardEvents()
    fake.autoplayBlocked(player)
    fake.flushEvents()
    observers.setRatio(iframe, 0.3)
    fake.state(player, PLAYING)
    fake.flushEvents()
    expect(count(player, 'pauseVideo')).toBe(0)
    expect(player.state).toBe(PLAYING)
  })
})
