// Plan M3b3c T3 (second fix): the real player may merge reports or skip one
// (a pause of a paused player). The fake's queue is replaced by the reports
// a merging or skipping player would send; our calls stay real.
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

const { PLAYING, PAUSED, BUFFERING } = PlayerState

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

async function mount() {
  render(
    <ConsentContext value={{ agreed: true, setAgreed: () => {} }}>
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

const calls = (player: FakePlayer, method: string) =>
  fake.callsOf(player).filter((m) => m === method).length

// Sends exactly these reports, in order, instead of the fake's own queue.
function reportOnly(player: FakePlayer, states: number[]) {
  fake.discardEvents()
  for (const state of states) fake.state(player, state)
  fake.flushEvents()
  // Whatever our code asked for meanwhile gets no report.
  fake.discardEvents()
}

// Away, back and away while playing: pause, play, pause before any report.
async function quickTurns() {
  const parts = await mount()
  observers.setRatio(parts.iframe, 0.51)
  expect(parts.player.state).toBe(PLAYING)
  fake.deferEvents()
  observers.setRatio(parts.iframe, 0.5)
  observers.setRatio(parts.iframe, 0.51)
  observers.setRatio(parts.iframe, 0.5)
  return parts
}

describe('merged or missing reports (T3, second fix)', () => {
  it('a pause that gets no PAUSED leaves the user’s reports readable', async () => {
    const { iframe, video, player } = await quickTurns()
    // The late PLAYING makes us pause once more; that pause is never answered.
    reportOnly(player, [PAUSED, BUFFERING, PLAYING])
    expect(calls(player, 'pauseVideo')).toBe(3)

    // The video plays; then the user plays the YouTube one by hand...
    observers.setRatio(iframe, 0.3)
    observers.setRatio(video, 0.51)
    expect(video.paused).toBe(false)
    reportOnly(player, [PLAYING])
    // ...it takes over: never two at once...
    expect(video.paused).toBe(true)
    // ...and the user's pause is the user's: no autoplay afterwards.
    reportOnly(player, [PAUSED])
    observers.setRatio(iframe, 0.51)
    expect(calls(player, 'playVideo')).toBe(2)
  })

  it('one PAUSED for two pauses is not the user’s pause', async () => {
    const { iframe, video, player } = await quickTurns()
    reportOnly(player, [PAUSED])
    observers.setRatio(video, 0.51)
    expect(video.paused).toBe(false)
    observers.setRatio(video, 0.3)
    observers.setRatio(iframe, 0.51)
    expect(calls(player, 'playVideo')).toBe(3)
  })

  it('a start reported only as PLAYING (no BUFFERING) is ours', async () => {
    const { iframe, video, player } = await mount()
    fake.deferEvents()
    observers.setRatio(video, 0.51)
    observers.setRatio(iframe, 0.51)
    expect(video.paused).toBe(true)
    reportOnly(player, [PLAYING])
    observers.setRatio(iframe, 0.5)
    expect(calls(player, 'pauseVideo')).toBe(1)
    expect(video.paused).toBe(true)
  })
})
