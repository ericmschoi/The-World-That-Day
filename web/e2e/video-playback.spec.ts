// Plan M3b3 T4 (2): video playback in a real Chromium, on the synthetic
// `world1988WithVideo` (one video in event ①, an undated copy in day_media).
// Nothing reaches the network: the video files are held unanswered (or
// aborted), so these tests watch play *attempts* — the calls to play() and
// pause() and `muted` at that moment — not decoding (plan risk 3).
import {
  expect,
  test,
  type BrowserContext,
  type Locator,
  type Page,
} from '@playwright/test'
import { exampleVideoFile } from '../src/mocks/fixtures/world/media'
import { world1988WithVideo } from '../src/mocks/fixtures/world/responses'
import type { WorldResponse } from '../src/world/types'
import { interceptNetwork, openDemo, type VideoRequests } from './network'

const DEMO_1988 = '1988년 9월 17일로 보기'
const SAME_DAY = '그날 찍힌 사진'
const UNDATED = '촬영 시점을 확인하지 못한 사진'
const EVENT_VIDEO = '「1988년 하계 올림픽」 관련 자료'
const DAY_VIDEO = '기록 영상'
const VIDEO_URLS = exampleVideoFile.video_sources.map((source) => source.url)

type Call = { kind: 'play' | 'pause'; label: string; muted: boolean }
type MediaLog = { calls: Call[]; maxPlaying: number }

// Runs in the page before any app code. Wraps play() and pause() to record
// each call with the element's name and `muted` at the moment of the call,
// then calls the real one. `maxPlaying` is the most videos ever seen with
// `paused === false` at once, sampled at every call, every media event and
// every animation frame.
function recordMedia() {
  const log: MediaLog = { calls: [], maxPlaying: 0 }
  ;(window as unknown as { __media: MediaLog }).__media = log
  const sample = () => {
    const playing = [...document.querySelectorAll('video')].filter(
      (video) => !video.paused,
    ).length
    log.maxPlaying = Math.max(log.maxPlaying, playing)
  }
  const proto = HTMLMediaElement.prototype
  const originalPlay = proto.play
  const originalPause = proto.pause
  proto.play = function (this: HTMLMediaElement) {
    const muted = this.muted
    const result = originalPlay.call(this)
    log.calls.push({
      kind: 'play',
      label: this.getAttribute('aria-label') ?? '',
      muted,
    })
    sample()
    return result
  }
  proto.pause = function (this: HTMLMediaElement) {
    const muted = this.muted
    originalPause.call(this)
    log.calls.push({
      kind: 'pause',
      label: this.getAttribute('aria-label') ?? '',
      muted,
    })
    sample()
  }
  for (const type of ['play', 'playing', 'pause']) {
    document.addEventListener(type, sample, true)
  }
  const tick = () => {
    sample()
    requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

const mediaLog = (page: Page) =>
  page.evaluate(() => (window as unknown as { __media: MediaLog }).__media)

const callsOf = async (page: Page, kind: Call['kind'], label: string) =>
  (await mediaLog(page)).calls.filter(
    (call) => call.kind === kind && call.label === label,
  )

const eventVideo = (page: Page) =>
  page.getByRole('list', { name: '사건 목록' }).locator('video')
const dayVideo = (page: Page) => page.getByLabel(DAY_VIDEO, { exact: true })

const toCenter = (video: Locator) =>
  video.evaluate((element) => element.scrollIntoView({ block: 'center' }))

const isPaused = (video: Locator) =>
  video.evaluate((element) => (element as HTMLVideoElement).paused)

// The share of the element inside the viewport, as the observer measures it.
const visibleRatio = (locator: Locator) =>
  locator.evaluate((element) => {
    const box = element.getBoundingClientRect()
    const shown =
      Math.min(box.bottom, window.innerHeight) - Math.max(box.top, 0)
    return box.height > 0 ? Math.max(0, shown) / box.height : 0
  })

// For the "nothing happens" cases: waits until a fresh observer of our own
// reports the element more than half visible, then a little longer so the
// app's observer and effects have certainly run.
const settleInView = async (page: Page, video: Locator) => {
  await video.evaluate(
    (element) =>
      new Promise<void>((resolve) => {
        const observer = new IntersectionObserver(
          (entries) => {
            if (entries.some((entry) => entry.intersectionRatio > 0.5)) {
              observer.disconnect()
              resolve()
            }
          },
          { threshold: [0.5, 0.51] },
        )
        observer.observe(element)
      }),
  )
  await page.waitForTimeout(300)
}

async function open(
  page: Page,
  context: BrowserContext,
  videos: VideoRequests,
  world: WorldResponse = world1988WithVideo,
) {
  await page.addInitScript(recordMedia)
  const outside = await interceptNetwork(context, world, { videos })
  await openDemo(page, DEMO_1988)
  return outside
}

let outside: Awaited<ReturnType<typeof interceptNetwork>>
let pageErrors: string[]

test.beforeEach(({ page }) => {
  pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
})

// (f) No uncaught exception, and no outside request got an answer.
test.afterEach(() => {
  expect(pageErrors).toEqual([])
  expect(outside.answered).toEqual([])
})

test.describe('video files held unanswered', () => {
  test.beforeEach(async ({ context, page }) => {
    outside = await open(page, context, 'hold')
  })

  test('(a) plays muted once in view, pauses once off screen', async ({
    page,
  }) => {
    const video = eventVideo(page)
    await expect(video).toHaveCount(1)

    await toCenter(video)
    await expect
      .poll(async () => (await callsOf(page, 'play', EVENT_VIDEO)).length)
      .toBe(1)
    const [play] = await callsOf(page, 'play', EVENT_VIDEO)
    expect(play.muted).toBe(true)
    expect(await isPaused(video)).toBe(false)
    expect(await video.getAttribute('controls')).not.toBeNull()
    // The first source was really asked for, and held.
    expect(outside.held).toContain(VIDEO_URLS[0])

    await page.evaluate(() => window.scrollTo(0, 0))
    expect(await visibleRatio(video)).toBeLessThanOrEqual(0.5)
    await expect
      .poll(async () => (await callsOf(page, 'pause', EVENT_VIDEO)).length)
      .toBe(1)
    expect(await isPaused(video)).toBe(true)
  })

  test('(b) never has more than one video playing', async ({ page }) => {
    const first = eventVideo(page)
    const second = dayVideo(page)
    await expect(second).toHaveCount(1)

    for (let round = 0; round < 3; round++) {
      for (const [now, other] of [
        [first, second],
        [second, first],
      ]) {
        await toCenter(now)
        await expect.poll(() => isPaused(now)).toBe(false)
        expect(await isPaused(other)).toBe(true)
      }
    }

    const log = await mediaLog(page)
    expect(log.maxPlaying).toBe(1)
    // Both took turns: three autoplays each, every one muted.
    const plays = log.calls.filter((call) => call.kind === 'play')
    expect(plays.filter((call) => call.label === EVENT_VIDEO)).toHaveLength(3)
    expect(plays.filter((call) => call.label === DAY_VIDEO)).toHaveLength(3)
    expect(plays.every((call) => call.muted)).toBe(true)
  })

  test('(g) has no sideways scroll', async ({ page }) => {
    const { scrollWidth, clientWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }))
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth)
  })
})

test.describe('no autoplay when the reader asks for less', () => {
  test('(c) under reduced motion: no play(), controls stay', async ({
    context,
    page,
  }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' })
    outside = await open(page, context, 'hold')
    expect(
      await page.evaluate(
        () => matchMedia('(prefers-reduced-motion: reduce)').matches,
      ),
    ).toBe(true)

    for (const video of [eventVideo(page), dayVideo(page)]) {
      await toCenter(video)
      await settleInView(page, video)
      expect(await video.getAttribute('controls')).not.toBeNull()
      expect(await isPaused(video)).toBe(true)
    }
    expect(
      (await mediaLog(page)).calls.filter((c) => c.kind === 'play'),
    ).toEqual([])
  })

  test('(d) with data saving on: no play()', async ({ context, page }) => {
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'connection', {
        configurable: true,
        value: { saveData: true },
      })
    })
    outside = await open(page, context, 'hold')
    expect(
      await page.evaluate(
        () =>
          (navigator as Navigator & { connection?: { saveData?: boolean } })
            .connection?.saveData,
      ),
    ).toBe(true)

    for (const video of [eventVideo(page), dayVideo(page)]) {
      await toCenter(video)
      await settleInView(page, video)
      expect(await video.getAttribute('controls')).not.toBeNull()
      expect(await isPaused(video)).toBe(true)
    }
    expect(
      (await mediaLog(page)).calls.filter((c) => c.kind === 'play'),
    ).toEqual([])
  })
})

test.describe('video files that fail', () => {
  test.beforeEach(async ({ context, page }) => {
    outside = await open(page, context, 'abort')
  })

  test('(e) hides both video cards and captions, leaves the rest', async ({
    page,
  }) => {
    const world = page.getByRole('region', { name: '그날의 세계' })
    await expect(world.locator('video')).toHaveCount(0)
    // Chromium tried every source before giving up.
    for (const url of VIDEO_URLS) expect(outside.aborted).toContain(url)

    // No caption is left behind: only the four photo cards remain.
    await expect(world.getByRole('figure')).toHaveCount(4)
    await expect(world.getByText(exampleVideoFile.attribution)).toHaveCount(0)
    await expect(world.getByLabel(DAY_VIDEO, { exact: true })).toHaveCount(0)

    // The undated photo copy keeps its heading.
    await expect(world.getByRole('heading', { level: 3 })).toHaveText([
      SAME_DAY,
      UNDATED,
    ])
    const undated = world
      .getByRole('heading', { level: 3, name: UNDATED })
      .locator('..')
    await expect(undated.getByRole('img')).toHaveCount(1)

    const first = world
      .getByRole('list', { name: '사건 목록' })
      .getByRole('listitem')
      .first()
    await expect(first).toContainText('제24회 서울 하계 올림픽 개막.')
    await expect(first.getByRole('img')).toHaveCount(2)
    await expect(first).toContainText('출처: 한국어 위키백과')

    await expect(world).not.toContainText(/오류|실패|재생할 수 없/)
  })
})

// Found in T4, beyond the plan's list: two videos that come into view in the
// same frame (a jump in scrolling, or both visible on arrival). M3a4b gives
// each event up to one video, so two events in a row can each carry one.
// The plan's rules ("보이면 음소거 자동 재생", "하나만 재생") mean exactly one
// of them plays.
const SECOND_EVENT_VIDEO = '「KBS 1TV」 관련 자료'
const twoEventVideos: WorldResponse = {
  ...world1988WithVideo,
  events: [
    world1988WithVideo.events[0],
    {
      ...world1988WithVideo.events[1],
      media: [
        {
          ...world1988WithVideo.events[0].media[2],
          id: 'example-video-file-second-event',
        },
      ],
    },
    ...world1988WithVideo.events.slice(2),
  ],
}

test.describe('two videos entering the view together', () => {
  test.beforeEach(async ({ context, page }) => {
    outside = await open(page, context, 'hold', twoEventVideos)
  })

  test('exactly one of them autoplays', async ({ page }) => {
    const list = page.getByRole('list', { name: '사건 목록' })
    const first = list.getByLabel(EVENT_VIDEO, { exact: true })
    const second = list.getByLabel(SECOND_EVENT_VIDEO, { exact: true })
    await expect(second).toHaveCount(1)

    // One jump that puts the middle of the pair at the middle of the screen.
    await page.evaluate(() => {
      const [a, b] = [...document.querySelectorAll('ul video')]
      const top = a.getBoundingClientRect().top + window.scrollY
      const bottom = b.getBoundingClientRect().bottom + window.scrollY
      window.scrollTo(0, (top + bottom) / 2 - window.innerHeight / 2)
    })
    expect(await visibleRatio(first)).toBeGreaterThan(0.5)
    expect(await visibleRatio(second)).toBeGreaterThan(0.5)

    await expect
      .poll(async () => {
        const paused = [await isPaused(first), await isPaused(second)]
        return paused.filter((value) => !value).length
      })
      .toBe(1)
    expect((await mediaLog(page)).maxPlaying).toBe(1)
  })
})

// Plan M3b3b T3 (3), on T0 (M3b3 review recommendation 1): a video the user
// starts while it shows less than half (so it is already "out of view")
// must still pause once it leaves the screen entirely, sound on or not.
// Event ①'s video is used; the day video sits below the events, so moving
// event ① off the bottom edge brings no other video into view.

// Puts the top `share` of the element at the bottom edge of the viewport.
const showTopShare = (video: Locator, share: number) =>
  video.evaluate((element, part) => {
    const box = element.getBoundingClientRect()
    const top = box.top + window.scrollY
    window.scrollTo(0, top - window.innerHeight + part * box.height)
  }, share)

// Scrolls up until the element is just below the bottom edge.
const pushBelowView = (video: Locator) =>
  video.evaluate((element) => {
    const top = element.getBoundingClientRect().top + window.scrollY
    window.scrollTo(0, Math.max(0, top - window.innerHeight - 40))
  })

// Waits for a fresh observer's first report, then a little longer so the
// app's observer and effects have certainly run.
const settle = async (page: Page, video: Locator) => {
  await video.evaluate(
    (element) =>
      new Promise<void>((resolve) => {
        const observer = new IntersectionObserver(() => {
          observer.disconnect()
          resolve()
        })
        observer.observe(element)
      }),
  )
  await page.waitForTimeout(300)
}

// The user's own play (as the controls would), resolved once the element's
// `play` event has been handled.
const userPlay = (video: Locator) =>
  video.evaluate(
    (element) =>
      new Promise<void>((resolve) => {
        element.addEventListener('play', () => resolve(), { once: true })
        ;(element as HTMLVideoElement).play().catch(() => {})
      }),
  )

const userUnmute = (video: Locator) =>
  video.evaluate(
    (element) =>
      new Promise<void>((resolve) => {
        element.addEventListener('volumechange', () => resolve(), {
          once: true,
        })
        ;(element as HTMLVideoElement).muted = false
      }),
  )

const isMuted = (video: Locator) =>
  video.evaluate((element) => (element as HTMLVideoElement).muted)

test.describe('a video the user starts under half visible (M3b3b T0)', () => {
  test.beforeEach(async ({ context, page }) => {
    outside = await open(page, context, 'hold')
  })

  for (const unmute of [false, true]) {
    test(`pauses when it leaves the screen${unmute ? ', sound on' : ''}`, async ({
      page,
    }) => {
      const video = eventVideo(page)
      await expect(video).toHaveCount(1)

      await showTopShare(video, 0.3)
      const ratio = await visibleRatio(video)
      expect(ratio).toBeGreaterThan(0.05)
      expect(ratio).toBeLessThanOrEqual(0.5)
      await settle(page, video)
      expect(await isPaused(video)).toBe(true)

      await userPlay(video)
      if (unmute) await userUnmute(video)
      expect(await isPaused(video)).toBe(false)
      expect(await isMuted(video)).toBe(!unmute)
      // Still under half: nothing pauses it yet.
      await page.waitForTimeout(300)
      expect(await isPaused(video)).toBe(false)
      const pausesBefore = (await callsOf(page, 'pause', EVENT_VIDEO)).length
      const dayPlaysBefore = (await callsOf(page, 'play', DAY_VIDEO)).length

      await pushBelowView(video)
      expect(await visibleRatio(video)).toBe(0)
      await expect
        .poll(async () => (await callsOf(page, 'pause', EVENT_VIDEO)).length)
        .toBe(pausesBefore + 1)
      expect(await isPaused(video)).toBe(true)
      const pause = (await callsOf(page, 'pause', EVENT_VIDEO)).at(-1)
      expect(pause?.muted).toBe(!unmute)
      // Paused by leaving the screen, not by another video taking over.
      expect((await callsOf(page, 'play', DAY_VIDEO)).length).toBe(
        dayPlaysBefore,
      )
      expect(await isPaused(dayVideo(page))).toBe(true)
      expect((await mediaLog(page)).maxPlaying).toBe(1)
    })
  }
})
