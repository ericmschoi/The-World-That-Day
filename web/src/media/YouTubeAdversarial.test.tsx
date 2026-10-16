// Plan M3b3c T4, rerun 2 (9a7d037): the reader's own play and pause mixed
// with ours while the player's reports are still on their way, in both
// fake modes for a pause before the start. Fake IntersectionObserver, media
// element and `YT`; no network.
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

const { PLAYING, PAUSED } = PlayerState
const MODES = ['none', 'paused'] as const

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

const count = (player: FakePlayer, method: string) =>
  fake.callsOf(player).filter((m) => m === method).length

describe('the reader’s play and pause among late reports (T4 rerun 2)', () => {
  it.each(MODES)(
    'a pause the user makes before our start is reported is the user’s (%s)',
    async (mode) => {
      const { iframe, player } = await mount()
      fake.setPauseBeforeStart(mode)
      fake.deferEvents()
      observers.setRatio(iframe, 0.51)
      // The start comes, then the reader pauses it with the controls.
      fake.state(player, PAUSED)
      fake.flushEvents()
      expect(player.state).toBe(PAUSED)
      observers.setRatio(iframe, 0.5)
      observers.setRatio(iframe, 0.51)
      expect(count(player, 'playVideo')).toBe(1)
    },
  )

  it.each(MODES)(
    'a tap while our cancelled start may still come: one silent pause at most, never two playing (%s)',
    async (mode) => {
      const { iframe, video, player } = await mount()
      fake.setPauseBeforeStart(mode)
      fake.deferEvents()
      observers.setRatio(iframe, 0.51)
      observers.setRatio(iframe, 0.3) // cancelled before any report
      observers.setRatio(video, 0.51)
      expect(video.paused).toBe(false)
      // The reader taps play on the player, still under half visible.
      fake.state(player, PLAYING)
      fake.flushEvents()
      // Our cancel plus at most one silent pause of a late start.
      expect(count(player, 'pauseVideo')).toBeLessThanOrEqual(2)
      const playing =
        (player.state === PLAYING ? 1 : 0) + (video.paused ? 0 : 1)
      expect(playing).toBeLessThanOrEqual(1)
    },
  )

  it('our play while the user plays it with sound: no restart, no mute', async () => {
    const { iframe, player } = await mount()
    observers.setRatio(iframe, 0.3)
    player.muted = false
    fake.state(player, PLAYING)
    observers.setRatio(iframe, 0.51)
    expect(count(player, 'playVideo')).toBe(0)
    expect(count(player, 'mute')).toBe(0)
    // Off screen it still pauses (M3b3b T0).
    observers.setRatio(iframe, 0)
    expect(count(player, 'pauseVideo')).toBe(1)
  })

  // T3 (3) / M3b3b T0 sends the PLAYING before the "out of view" report.
  // Here the reader taps play under half visible and scrolls it off screen
  // before the player's PLAYING arrives.
  it('a user play reported after it left the screen is still paused', async () => {
    const { iframe, video, player } = await mount()
    observers.setRatio(iframe, 0.3)
    observers.setRatio(video, 0.51)
    expect(video.paused).toBe(false)
    fake.deferEvents()
    fake.state(player, PLAYING) // the tap, reported late
    observers.setRatio(iframe, 0)
    fake.flushEvents()
    // It must not play off screen.
    expect(count(player, 'pauseVideo')).toBe(1)
    expect(player.state).not.toBe(PLAYING)
  })
})
