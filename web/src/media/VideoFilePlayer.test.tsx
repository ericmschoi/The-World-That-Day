import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { videoFile } from '../mocks/fixtures/media/modes'
import { sameDayPhoto } from '../mocks/fixtures/media/photos'
import {
  installIntersectionObserverFake,
  installMediaElementFake,
  type IntersectionObserverFake,
  type MediaElementFake,
} from '../test/mediaFakes'
import { MediaItem } from './MediaItem'
import { PlaybackProvider } from './PlaybackProvider'
import type { Media } from './types'

const ALT = '기록 영상'

const patch = (media: Media, fields: Record<string, unknown>) =>
  ({ ...media, ...fields }) as Media

// Two transcodes, small first, like the server sends them (M3a4b).
const twoSources = patch(videoFile, {
  id: 'example-commons-video-0002',
  video_sources: [
    { url: 'https://media.example.org/v.360p.webm', mime: 'video/webm' },
    { url: 'https://media.example.org/v.480p.webm', mime: 'video/webm' },
  ],
})

let observers: IntersectionObserverFake
let media: MediaElementFake

beforeEach(() => {
  observers = installIntersectionObserverFake()
  media = installMediaElementFake()
})

afterEach(() => {
  observers.uninstall()
  media.uninstall()
  Reflect.deleteProperty(window, 'matchMedia')
  Reflect.deleteProperty(navigator, 'connection')
  vi.restoreAllMocks()
})

// Each render gets its own provider, so no coordinator is shared by tests.
const renderVideos = (items: Media[], onUnavailable?: (id: string) => void) =>
  render(
    <PlaybackProvider>
      {items.map((item) => (
        <MediaItem
          key={item.id}
          media={item}
          fallbackAlt={ALT}
          onUnavailable={onUnavailable}
        />
      ))}
    </PlaybackProvider>,
  )

const videos = () =>
  [...document.querySelectorAll('video')] as HTMLVideoElement[]

describe('VideoFilePlayer element (T2 (1)·(2))', () => {
  it('renders a muted, inline video with controls and its caption', () => {
    renderVideos([videoFile])
    const [video] = videos()
    expect(video).toHaveAttribute('controls')
    expect(video).toHaveAttribute('playsinline')
    expect(video).toHaveAttribute('preload', 'metadata')
    expect(video).toHaveAttribute('poster', videoFile.thumbnail_url)
    expect(video).toHaveAttribute('width', String(videoFile.thumbnail_width))
    expect(video).toHaveAttribute('height', String(videoFile.thumbnail_height))
    expect(video).toHaveAccessibleName(ALT)
    expect(video.muted).toBe(true)
    expect(video.defaultMuted).toBe(true)
    for (const name of ['autoplay', 'loop', 'crossorigin']) {
      expect(video).not.toHaveAttribute(name)
    }
    const figure = screen.getByRole('figure')
    expect([...figure.children].map((child) => child.tagName)).toEqual([
      'VIDEO',
      'FIGCAPTION',
    ])
  })

  it('lists the sources in server order with their mime type', () => {
    renderVideos([twoSources])
    const sources = [...document.querySelectorAll('video > source')]
    expect(sources.map((s) => s.getAttribute('src'))).toEqual([
      'https://media.example.org/v.360p.webm',
      'https://media.example.org/v.480p.webm',
    ])
    expect(sources.map((s) => s.getAttribute('type'))).toEqual([
      'video/webm',
      'video/webm',
    ])
  })

  it.each([
    ['an empty string', ''],
    ['a number', 42],
    ['null', null],
  ])('leaves out type when mime is %s', (_case, mime) => {
    renderVideos([
      patch(videoFile, {
        video_sources: [{ url: 'https://media.example.org/v.webm', mime }],
      }),
    ])
    expect(document.querySelector('source')).not.toHaveAttribute('type')
  })

  it('uses url as the only source when video_sources is empty', () => {
    renderVideos([
      patch(videoFile, {
        video_sources: [],
        url: 'https://media.example.org/whole.webm',
      }),
    ])
    const sources = document.querySelectorAll('source')
    expect(sources).toHaveLength(1)
    expect(sources[0]).toHaveAttribute(
      'src',
      'https://media.example.org/whole.webm',
    )
    expect(sources[0]).not.toHaveAttribute('type')
  })

  it('has no poster without a thumbnail', () => {
    renderVideos([patch(videoFile, { thumbnail_url: null })])
    expect(videos()[0]).not.toHaveAttribute('poster')
  })
})

describe('VideoFilePlayer playback (T2 (3)·(4)·(6)·(7))', () => {
  it('plays muted past half visible and pauses at half', () => {
    renderVideos([videoFile])
    const [video] = videos()
    observers.setRatio(video, 0.51)
    expect(media.play).toHaveBeenCalledTimes(1)
    expect(media.mutedAtPlay).toEqual([true])
    observers.setRatio(video, 0.5)
    expect(media.pause).toHaveBeenCalledTimes(1)
    expect(video.paused).toBe(true)
  })

  it('does not autoplay under reduced motion, keeping the controls', () => {
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: (query: string) => ({
        matches: query === '(prefers-reduced-motion: reduce)',
      }),
    })
    renderVideos([videoFile])
    observers.setRatio(videos()[0], 0.51)
    expect(media.play).not.toHaveBeenCalled()
    expect(videos()[0]).toHaveAttribute('controls')
  })

  it('does not autoplay when data saving is on', () => {
    Object.defineProperty(navigator, 'connection', {
      configurable: true,
      value: { saveData: true },
    })
    renderVideos([videoFile])
    observers.setRatio(videos()[0], 0.51)
    expect(media.play).not.toHaveBeenCalled()
  })

  it('pauses the first video when the second comes into view', () => {
    renderVideos([videoFile, twoSources])
    const [first, second] = videos()
    observers.setRatio(first, 0.51)
    observers.setRatio(second, 0.51)
    expect(media.pause.mock.contexts).toEqual([first])
    expect(media.play.mock.contexts).toEqual([first, second])
    expect(videos().filter((video) => !video.paused)).toEqual([second])
  })

  // Browsers fire `play` later than `play()`: A's event arrives after B took
  // over, while A is paused again. It must not pause B (e2e "together").
  it('keeps one playing when two come into view before any event fires', () => {
    media.deferEvents()
    renderVideos([videoFile, twoSources])
    const [first, second] = videos()
    observers.setRatio(first, 0.51)
    observers.setRatio(second, 0.51)
    media.flushEvents()
    expect(first.paused).toBe(true)
    expect(second.paused).toBe(false)
    expect(media.pause.mock.contexts).toEqual([first])
  })

  it('lets a user who presses play take over with deferred events', () => {
    media.deferEvents()
    renderVideos([videoFile, twoSources])
    const [first, second] = videos()
    observers.setRatio(first, 0.51)
    media.flushEvents()
    act(() => void second.play())
    media.flushEvents()
    expect(first.paused).toBe(true)
    expect(second.paused).toBe(false)
  })

  it('pauses the first video when the user starts the second', () => {
    renderVideos([videoFile, twoSources])
    const [first, second] = videos()
    observers.setRatio(first, 0.51)
    // The user presses play on the second video's own controls.
    act(() => void second.play())
    expect(media.pause.mock.contexts).toEqual([first])
    expect(first.paused).toBe(true)
    expect(second.paused).toBe(false)
  })

  it('does not autoplay again after the user paused it, until they play', () => {
    renderVideos([videoFile])
    const [video] = videos()
    observers.setRatio(video, 0.51)
    act(() => video.pause()) // the user's pause, not ours
    observers.setRatio(video, 0.4)
    observers.setRatio(video, 0.51)
    expect(media.play).toHaveBeenCalledTimes(1)

    act(() => void video.play()) // the user plays it again
    observers.setRatio(video, 0.4)
    observers.setRatio(video, 0.51)
    expect(media.play).toHaveBeenCalledTimes(3)
  })

  it('keeps the sound on once the user turned it on', () => {
    renderVideos([videoFile])
    const [video] = videos()
    observers.setRatio(video, 0.51)
    act(() => {
      video.muted = false
      fireEvent(video, new Event('volumechange'))
    })
    observers.setRatio(video, 0.4)
    observers.setRatio(video, 0.51)
    expect(media.mutedAtPlay).toEqual([true, false])
  })
})

describe('VideoFilePlayer when play() is refused (T2 (5))', () => {
  it('keeps the card and caption quietly', async () => {
    type Proc = { on(e: string, f: () => void): void; off: Proc['on'] }
    const proc = (globalThis as unknown as { process: Proc }).process
    const unhandled = vi.fn()
    proc.on('unhandledRejection', unhandled)
    const spies = [vi.spyOn(console, 'error'), vi.spyOn(console, 'warn')]
    media.rejectPlayWith(new DOMException('blocked', 'NotAllowedError'))

    renderVideos([videoFile])
    observers.setRatio(videos()[0], 0.51)
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)))
    proc.off('unhandledRejection', unhandled)

    expect(media.play).toHaveBeenCalledTimes(1)
    expect(videos()).toHaveLength(1)
    expect(screen.getByRole('figure')).toHaveTextContent('View original')
    expect(screen.queryByRole('alert')).toBeNull()
    expect(unhandled).not.toHaveBeenCalled()
    for (const spy of spies) expect(spy).not.toHaveBeenCalled()
  })
})

describe('VideoFilePlayer when it cannot play (T2 (8))', () => {
  const photoAndVideo = () => {
    const onUnavailable = vi.fn()
    renderVideos([sameDayPhoto, twoSources], onUnavailable)
    return onUnavailable
  }

  it('hides the card and caption when the last source fails', () => {
    const onUnavailable = photoAndVideo()
    const [video] = videos()
    observers.setRatio(video, 0.51)
    const sources = video.querySelectorAll('source')
    fireEvent.error(sources[sources.length - 1])

    expect(videos()).toHaveLength(0)
    expect(screen.getAllByRole('figure')).toHaveLength(1)
    expect(screen.getByRole('img')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /View original/ })).toHaveLength(
      1,
    )
    expect(onUnavailable).toHaveBeenCalledTimes(1)
    expect(onUnavailable).toHaveBeenCalledWith(twoSources.id)
    expect(document.body).not.toHaveTextContent(/오류|실패|재생할 수 없/)
  })

  it('hides the card when the video itself fails', () => {
    const onUnavailable = photoAndVideo()
    fireEvent.error(videos()[0])
    fireEvent.error(document.createElement('video')) // unrelated, ignored
    expect(videos()).toHaveLength(0)
    expect(screen.getByRole('img')).toBeInTheDocument()
    expect(onUnavailable).toHaveBeenCalledTimes(1)
    expect(onUnavailable).toHaveBeenCalledWith(twoSources.id)
  })

  it('keeps the card when only the first source fails', () => {
    const onUnavailable = photoAndVideo()
    fireEvent.error(videos()[0].querySelector('source')!)
    expect(videos()).toHaveLength(1)
    expect(onUnavailable).not.toHaveBeenCalled()
  })

  it('leaves the coordinator: a failed video is never paused again', () => {
    renderVideos([twoSources, videoFile])
    const [failing, other] = videos()
    observers.setRatio(failing, 0.51)
    const sources = failing.querySelectorAll('source')
    fireEvent.error(sources[sources.length - 1])
    media.pause.mockClear()
    observers.setRatio(other, 0.51)
    expect(media.pause).not.toHaveBeenCalled()
    expect(other.paused).toBe(false)
  })
})

describe('VideoFilePlayer caption (T2 (9))', () => {
  it('says 촬영 시점 미상 for an unknown capture time, never 같은 날', () => {
    renderVideos([
      patch(videoFile, {
        captured_at: null,
        date_precision: null,
        date_relation: { kind: 'unknown', years: 0 },
      }),
    ])
    const caption = screen.getByRole('figure').querySelector('figcaption')!
    expect(caption).toHaveTextContent('Capture time unknown')
    expect(caption).not.toHaveTextContent('Same day')
    expect(caption).toHaveTextContent(videoFile.attribution)
    expect(caption).toHaveTextContent('CC BY-SA 4.0')
    expect(
      screen.getByRole('link', { name: /^View original/ }),
    ).toBeInTheDocument()
  })
})

// M3b3b T0 (M3b3 review recommendations 1 and 2).
describe('VideoFilePlayer releases on every out-of-view report (T0)', () => {
  // The user starts a video that is visible but not past half, so it was
  // already out of view; then it leaves the screen entirely.
  const userPlaysAtThirdThenLeaves = (unmute: boolean) => {
    renderVideos([videoFile])
    const [video] = videos()
    observers.setRatio(video, 0.3)
    act(() => void video.play()) // the user's own controls
    if (unmute) {
      act(() => {
        video.muted = false
        fireEvent(video, new Event('volumechange'))
      })
    }
    media.pause.mockClear()
    observers.setRatio(video, 0)
    return video
  }

  it('pauses a video the user started once it leaves the screen (1)', () => {
    const video = userPlaysAtThirdThenLeaves(false)
    expect(media.pause).toHaveBeenCalledTimes(1)
    expect(media.pause.mock.contexts).toEqual([video])
    expect(video.paused).toBe(true)
  })

  it('pauses it the same way after the user turned the sound on (2)', () => {
    const video = userPlaysAtThirdThenLeaves(true)
    expect(media.pause).toHaveBeenCalledTimes(1)
    expect(video.paused).toBe(true)
  })

  it('does not count that pause as the user stopping it (3)', () => {
    const video = userPlaysAtThirdThenLeaves(true)
    expect(video.paused).toBe(true)
    media.play.mockClear()
    observers.setRatio(video, 0.51)
    expect(media.play).toHaveBeenCalledTimes(1)
    expect(media.play.mock.contexts).toEqual([video])
    expect(video.muted).toBe(false)
  })

  it('never pauses another playing video on repeated reports (4)', () => {
    renderVideos([videoFile, twoSources])
    const [first, second] = videos()
    observers.setRatio(second, 0.51)
    media.pause.mockClear()
    observers.setRatio(first, 0.3)
    observers.setRatio(first, 0.1)
    observers.setRatio(first, 0)
    expect(media.pause).not.toHaveBeenCalled()
    expect(second.paused).toBe(false)
  })

  it('plays once on two in-view reports in a row (5)', () => {
    renderVideos([videoFile])
    const [video] = videos()
    observers.setRatio(video, 0.51)
    observers.setRatio(video, 0.8)
    expect(media.play).toHaveBeenCalledTimes(1)
  })
})

describe('VideoFilePlayer counts its own pauses (T0 (6))', () => {
  it('reads two of our pause events as ours, then a real one as the user', () => {
    media.deferEvents()
    renderVideos([videoFile])
    const [video] = videos()
    observers.setRatio(video, 0.51)
    media.flushEvents()
    // Our pause() → play() → pause() before any of their events fire.
    observers.setRatio(video, 0.5)
    observers.setRatio(video, 0.51)
    observers.setRatio(video, 0.5)
    media.flushEvents()
    expect(media.play).toHaveBeenCalledTimes(2)
    observers.setRatio(video, 0.51)
    expect(media.play).toHaveBeenCalledTimes(3)
    media.flushEvents()

    act(() => video.pause()) // the user's pause, not ours
    media.flushEvents()
    observers.setRatio(video, 0.4)
    observers.setRatio(video, 0.51)
    expect(media.play).toHaveBeenCalledTimes(3)
  })
})
