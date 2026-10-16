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
          <MediaItem key={item.id} media={item} fallbackAlt="기록 영상" />
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
