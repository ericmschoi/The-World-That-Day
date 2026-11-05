// Plan M3b3 T4 (1): checks the T1–T3 tests do not make on their own. The
// story screen's one coordinator covers the event video and the day_media
// video together (T1 (4) with T2 (4)), video names carry no date words
// (T2 (9), M3b2b rule), and no IntersectionObserver means no autoplay on the
// real world screen. MSW only; the demo date with the synthetic video fixture.
import { act, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { world1988WithVideo } from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import { StoryScreen } from '../screens/StoryScreen'
import {
  installIntersectionObserverFake,
  installMediaElementFake,
  type IntersectionObserverFake,
  type MediaElementFake,
} from '../test/mediaFakes'

const DATE_WORDS = [
  '그날',
  'Same day',
  'that day',
  '당일',
  '1988-09-17',
  '1988년 9월 17일',
  'September 17',
  '9월 17일',
]

let observers: IntersectionObserverFake | null = null
let media: MediaElementFake | null = null

afterEach(() => {
  observers?.uninstall()
  media?.uninstall()
  observers = null
  media = null
  vi.restoreAllMocks()
})

const world = () => screen.getByRole('region', { name: 'The world that day' })

async function showStory() {
  server.use(
    http.get('/api/story/world', () => HttpResponse.json(world1988WithVideo)),
  )
  render(<StoryScreen date="1988-09-17" onRestart={() => {}} />)
  await waitFor(() => expect(world()).not.toHaveAttribute('aria-busy'))
  const eventVideo = within(screen.getByRole('list', { name: 'Events' }))
    .getAllByRole('listitem')[0]
    .querySelector('video')!
  const dayVideo = within(world()).getByLabelText('Archive video')
  return { eventVideo, dayVideo: dayVideo as HTMLVideoElement }
}

const playing = () =>
  [...document.querySelectorAll('video')].filter((video) => !video.paused)

describe('one coordinator for the whole story screen', () => {
  it('pauses the event video when the day_media video comes into view', async () => {
    observers = installIntersectionObserverFake()
    media = installMediaElementFake()
    const { eventVideo, dayVideo } = await showStory()
    expect(eventVideo).not.toBe(dayVideo)

    observers.setRatio(eventVideo, 0.51)
    expect(playing()).toEqual([eventVideo])
    observers.setRatio(dayVideo, 0.51)
    expect(playing()).toEqual([dayVideo])
    expect(media.pause.mock.contexts).toEqual([eventVideo])
    expect(media.mutedAtPlay).toEqual([true, true])
  })

  it('pauses the day_media video when the user starts the event video', async () => {
    observers = installIntersectionObserverFake()
    media = installMediaElementFake()
    const { eventVideo, dayVideo } = await showStory()

    observers.setRatio(dayVideo, 0.51)
    act(() => void eventVideo.play()) // the user's own controls
    expect(playing()).toEqual([eventVideo])
    expect(media.pause.mock.contexts).toEqual([dayVideo])
  })
})

describe('video names', () => {
  it('name no date in either video', async () => {
    const { eventVideo, dayVideo } = await showStory()
    expect(dayVideo).toHaveAccessibleName('Archive video')
    expect(eventVideo).toHaveAccessibleName(
      'Material about “1988년 하계 올림픽”',
    )
    for (const video of [eventVideo, dayVideo]) {
      const name = video.getAttribute('aria-label') ?? ''
      for (const word of DATE_WORDS) expect(name).not.toContain(word)
    }
  })
})

describe('without IntersectionObserver', () => {
  it('never calls play() on the world screen and stays quiet', async () => {
    expect(globalThis.IntersectionObserver).toBeUndefined()
    const play = vi
      .spyOn(HTMLMediaElement.prototype, 'play')
      .mockResolvedValue(undefined)
    const spies = [vi.spyOn(console, 'error'), vi.spyOn(console, 'warn')]
    await showStory()
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)))
    expect(document.querySelectorAll('video')).toHaveLength(2)
    expect(play).not.toHaveBeenCalled()
    for (const spy of spies) expect(spy).not.toHaveBeenCalled()
  })
})
