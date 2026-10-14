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
) {
  await page.addInitScript(recordMedia)
  const outside = await interceptNetwork(context, world1988WithVideo, {
    videos,
  })
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
