// Plan M3b3c T4: found beyond the T3 list. T3 (7) cancels one start before
// its report; here the player is sent away, back and away again before any
// report arrives (quick scrolling), in both fake modes for a pause before
// the start. Our own pauses must never read as the user's (T3 (5)·(6)).
import { act, render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ConsentContext } from '../consent/consent'
import { videoFile } from '../mocks/fixtures/media/modes'
import { exampleYoutubeEmbed } from '../mocks/fixtures/world/youtube'
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

const { PLAYING } = PlayerState

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
})

const agreed = { agreed: true, setAgreed: () => {} }

async function mount() {
  render(
    <ConsentContext value={agreed}>
      <PlaybackProvider>
        {[videoFile, exampleYoutubeEmbed].map((item) => (
          <MediaItem key={item.id} media={item} fallbackAlt="Archive video" />
        ))}
      </PlaybackProvider>
    </ConsentContext>,
  )
  await act(async () => {})
  for (const player of fake.players) fake.ready(player)
  const iframe = document.querySelector('iframe')!
  const video = document.querySelector('video') as HTMLVideoElement
  return { iframe, video, player: fake.playersFor(iframe)[0] }
}

const plays = (player: FakePlayer) =>
  fake.callsOf(player).filter((method) => method === 'playVideo').length

describe('away, back and away before any report (T4, beyond T3 (7))', () => {
  it.each(['none', 'paused'] as const)(
    'two cancelled starts are not the user’s pause (pause before start: %s)',
    async (mode) => {
      const { iframe, video, player } = await mount()
      fake.setPauseBeforeStart(mode)
      fake.deferEvents()
      observers.setRatio(iframe, 0.51)
      observers.setRatio(iframe, 0.5)
      observers.setRatio(iframe, 0.51)
      observers.setRatio(iframe, 0.5)
      expect(plays(player)).toBe(2)
      // The video takes over while the reports are still on their way.
      observers.setRatio(video, 0.51)
      expect(video.paused).toBe(false)
      fake.flushEvents()
      expect(player.state).not.toBe(PLAYING)
      expect(video.paused).toBe(false)

      // Nothing the user did: it autoplays when it comes back.
      observers.setRatio(video, 0.3)
      observers.setRatio(iframe, 0.51)
      expect(plays(player)).toBe(3)
    },
  )
})

// T4 rerun, probing 22fac92: in mode 'paused' the first start's BUFFERING
// arrives before our second pause, so that pause meets a buffering player.
// The unit fake cannot let part of its queue through, so the queue is
// replaced by the same reports in the order the fake would send them (the
// e2e "quick turns, fake mode paused, 1 report(s) early" does the same with
// the real queue).
describe('a pause that meets the second start buffering (T4 rerun)', () => {
  it('is not read as the user’s pause (pause before start: paused)', async () => {
    const { iframe, video, player } = await mount()
    const { BUFFERING, PAUSED } = PlayerState
    fake.setPauseBeforeStart('paused')
    fake.deferEvents()
    observers.setRatio(iframe, 0.51) // play: BUFFERING, PLAYING queued
    observers.setRatio(iframe, 0.5) // cancel: PLAYING dropped, PAUSED queued
    observers.setRatio(iframe, 0.51) // play: BUFFERING, PLAYING queued
    expect(plays(player)).toBe(2)

    // Only the first BUFFERING arrives now.
    fake.discardEvents()
    fake.state(player, BUFFERING)
    fake.flushEvents()
    observers.setRatio(iframe, 0.5) // our pause, the player buffering
    expect(fake.callsOf(player).filter((m) => m === 'pauseVideo')).toHaveLength(
      2,
    )
    observers.setRatio(video, 0.51)
    expect(video.paused).toBe(false)

    // Then the rest, in queue order: the cancel's PAUSED, the second start,
    // the PAUSED for our last pause (plus any our code asks for).
    fake.discardEvents()
    for (const state of [PAUSED, BUFFERING, PLAYING, PAUSED]) {
      fake.state(player, state)
    }
    fake.flushEvents()
    expect(player.state).toBe(PAUSED)
    expect(video.paused).toBe(false)

    // Nothing the user did: it autoplays when it comes back.
    observers.setRatio(video, 0.3)
    observers.setRatio(iframe, 0.51)
    expect(plays(player)).toBe(3)
  })
})

// T4 rerun, more orderings around a pending start (all expected to hold).
describe('other reports during a pending start (T4 rerun)', () => {
  const { BUFFERING, PAUSED, ENDED } = PlayerState
  const pauses = (player: FakePlayer) =>
    fake.callsOf(player).filter((m) => m === 'pauseVideo').length

  it('ENDED while a start is pending: no autoplay, a later tap is the user’s', async () => {
    const { iframe, video, player } = await mount()
    fake.deferEvents()
    observers.setRatio(iframe, 0.51)
    fake.discardEvents()
    fake.state(player, BUFFERING)
    fake.state(player, ENDED)
    fake.flushEvents()
    observers.setRatio(iframe, 0.5)
    observers.setRatio(iframe, 0.51)
    expect(plays(player)).toBe(1)

    // The video plays; then the user starts the player by hand.
    observers.setRatio(iframe, 0.3)
    observers.setRatio(video, 0.51)
    expect(video.paused).toBe(false)
    const before = pauses(player)
    fake.state(player, PLAYING)
    fake.flushEvents()
    expect(pauses(player)).toBe(before)
    expect(video.paused).toBe(true)
  })

  it.each(['none', 'paused'] as const)(
    'a user pause after our cancelled start is still the user’s (%s)',
    async (mode) => {
      const { iframe, player } = await mount()
      fake.setPauseBeforeStart(mode)
      fake.deferEvents()
      observers.setRatio(iframe, 0.51)
      observers.setRatio(iframe, 0.5)
      fake.flushEvents()
      // The user plays it by hand under half visible, then pauses it.
      observers.setRatio(iframe, 0.3)
      fake.state(player, PLAYING)
      fake.flushEvents()
      expect(player.state).toBe(PLAYING)
      fake.state(player, PAUSED)
      fake.flushEvents()
      observers.setRatio(iframe, 0.51)
      expect(plays(player)).toBe(1)
    },
  )

  it.each(['none', 'paused'] as const)(
    'a user tap after a cancel, before its answer, takes over (%s)',
    async (mode) => {
      const { iframe, video, player } = await mount()
      fake.setPauseBeforeStart(mode)
      fake.deferEvents()
      observers.setRatio(iframe, 0.51)
      observers.setRatio(iframe, 0.3)
      observers.setRatio(video, 0.51)
      expect(video.paused).toBe(false)
      fake.flushEvents()
      // Whatever came of our start, it is not playing now.
      expect(player.state).not.toBe(PLAYING)
      expect(video.paused).toBe(false)
      fake.state(player, PLAYING)
      fake.flushEvents()
      expect(player.state).toBe(PLAYING)
      expect(video.paused).toBe(true)
    },
  )
})
