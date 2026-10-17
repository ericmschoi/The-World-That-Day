// Plan M3b3c T4 (2)·(3): the YouTube cards in a real Chromium, on the
// synthetic `world1988WithYoutube` (event ①: Commons video + Daehan News,
// event ②: an undated YouTube record, day_media: an undated Commons video).
// Nothing reaches the network: every outside request is aborted, so the
// iframes show an empty error page and the IFrame API is a fake `YT`
// (e2e/youtubeFake.ts) put in before the app. These tests watch sizes,
// places and playback *calls*, never real YouTube playback (plan T5/T6).
import {
  expect,
  test,
  type BrowserContext,
  type Locator,
  type Page,
} from '@playwright/test'
import { YOUTUBE_EMBED_ORIGIN } from '../src/external/urls'
import type { Media } from '../src/media/types'
import {
  world1995,
  world2002WithMedia,
} from '../src/mocks/fixtures/world/responses'
import type { WorldResponse } from '../src/world/types'
import { exampleVideoFile } from '../src/mocks/fixtures/world/media'
import {
  exampleDaehanNews,
  exampleYoutubeUndated,
  world1988WithYoutube,
} from '../src/mocks/fixtures/world/youtube'
import { interceptNetwork, openDemo } from './network'
import {
  installFakeYT,
  ytCount,
  ytCreatedFor,
  ytDeliver,
  ytError,
  ytHold,
  ytMaxPlaying,
  ytPlayback,
  ytPlayingNow,
  ytQueued,
  ytRecord,
  ytResume,
  ytUserPlay,
  type FakeOptions,
} from './youtubeFake'

const DEMO_1988 = '1988년 9월 17일로 보기'
const CONSENT_LABEL = '개인정보처리방침에 동의합니다 (필수)'
const EVENT_1 = '「1988년 하계 올림픽」 관련 자료'
const EVENT_2 = '「KBS 1TV」 관련 자료'
const IFRAME_API = 'https://www.youtube.com/iframe_api'
const EMBED_URLS = [
  exampleDaehanNews.embed_url,
  exampleYoutubeUndated.embed_url,
]
const VIDEO_URLS = exampleVideoFile.video_sources.map((source) => source.url)
// Every file URL the fixtures' media name (photos, posters, video files,
// players): the only outside requests the page may make, all stopped.
const FIXTURE_MEDIA: Media[] = [
  world1988WithYoutube,
  world2002WithMedia,
  world1995,
].flatMap((story: WorldResponse): Media[] => [
  ...story.events.flatMap((event) => event.media),
  ...story.day_media,
])
const FIXTURE_FILES = FIXTURE_MEDIA.flatMap((media) => [
  media.url,
  media.thumbnail_url,
  media.embed_url,
  ...(media.video_sources ?? []).map((source) => source.url),
])
const ERROR_TEXT = /오류|실패|재생할 수 없/
const PLAYING = 1

const world = (page: Page) => page.getByRole('region', { name: '그날의 세계' })
const daehan = (page: Page) => page.locator(`iframe[title="${EVENT_1}"]`)
const undated = (page: Page) => page.locator(`iframe[title="${EVENT_2}"]`)
const commonsVideo = (page: Page) =>
  page.getByRole('list', { name: '사건 목록' }).locator('video')

const isYouTubeHost = (url: string) =>
  /(^|\.)(youtube\.com|youtube-nocookie\.com|ytimg\.com|googlevideo\.com)$/.test(
    new URL(url).hostname,
  )

const toCenter = (target: Locator) =>
  target.evaluate((element) => element.scrollIntoView({ block: 'center' }))

// Brings the element fully into view at one edge of the viewport.
// Scrolls so the element is fully visible and every other player (iframe
// or <video>) shows at most 40%, so only this one counts as "in view". At
// 360px a centred card leaves its neighbour more than half visible too.
const showAlone = (target: Locator) =>
  target.evaluate((element) => {
    const ratio = (el: Element) => {
      const box = el.getBoundingClientRect()
      const shown =
        Math.min(box.bottom, window.innerHeight) - Math.max(box.top, 0)
      return box.height > 0 ? Math.max(0, shown) / box.height : 0
    }
    const others = [...document.querySelectorAll('iframe, video')].filter(
      (el) => el !== element,
    )
    const box = element.getBoundingClientRect()
    const top = box.top + window.scrollY
    const slack = window.innerHeight - box.height
    // From centred outwards, both ways, 8px at a time.
    for (let step = 0; step <= slack / 2; step += 8) {
      for (const offset of [slack / 2 - step, slack / 2 + step]) {
        window.scrollTo(0, top - offset)
        if (ratio(element) === 1 && others.every((el) => ratio(el) <= 0.4)) {
          return
        }
      }
    }
    throw new Error('no scroll position shows this player alone')
  })

const isPaused = (video: Locator) =>
  video.evaluate((element) => (element as HTMLVideoElement).paused)

// The share of the element inside the viewport, as the observer measures it.
const visibleRatio = (target: Locator) =>
  target.evaluate((element) => {
    const box = element.getBoundingClientRect()
    const shown =
      Math.min(box.bottom, window.innerHeight) - Math.max(box.top, 0)
    return box.height > 0 ? Math.max(0, shown) / box.height : 0
  })

// Puts the top `share` of the element at the bottom edge of the viewport.
const showTopShare = (target: Locator, share: number) =>
  target.evaluate((element, part) => {
    const box = element.getBoundingClientRect()
    const top = box.top + window.scrollY
    window.scrollTo(0, top - window.innerHeight + part * box.height)
  }, share)

// Scrolls up until the element is just below the bottom edge.
const pushBelowView = (target: Locator) =>
  target.evaluate((element) => {
    const top = element.getBoundingClientRect().top + window.scrollY
    window.scrollTo(0, Math.max(0, top - window.innerHeight - 40))
  })

// Waits for a fresh observer's first report past `ratio` (or any report),
// then a little longer so the app's observer and effects have run.
const settle = async (page: Page, target: Locator, ratio = -1) => {
  await target.evaluate(
    (element, min) =>
      new Promise<void>((resolve) => {
        const observer = new IntersectionObserver(
          (entries) => {
            if (entries.some((entry) => entry.intersectionRatio > min)) {
              observer.disconnect()
              resolve()
            }
          },
          { threshold: [0, 0.5, 0.51] },
        )
        observer.observe(element)
      }),
    ratio,
  )
  await page.waitForTimeout(300)
}

const ytState = async (iframe: Locator) => (await ytRecord(iframe))?.reported

let outside: Awaited<ReturnType<typeof interceptNetwork>>
let pageErrors: string[]
let youtubeRequests: { url: string; referer: string | null }[]
// Extra outside requests a test expects to be stopped (only (h)).
let alsoAborted: string[]

test.beforeEach(({ context, page }) => {
  pageErrors = []
  youtubeRequests = []
  alsoAborted = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  context.on('request', async (request) => {
    if (!isYouTubeHost(request.url())) return
    const entry = { url: request.url(), referer: null as string | null }
    youtubeRequests.push(entry)
    entry.referer = await request.headerValue('referer').catch(() => null)
  })
})

// (k) No uncaught error, no outside request answered, and the only ones
// stopped are files the fixture names (the embed URLs, the Commons video
// files, and the photos and poster the plan's list leaves out) and, in (h)
// alone, the API script.
test.afterEach(() => {
  expect(pageErrors).toEqual([])
  expect(outside.answered).toEqual([])
  const allowed = new Set([...FIXTURE_FILES, ...alsoAborted])
  for (const url of [...EMBED_URLS, ...VIDEO_URLS]) {
    expect(allowed.has(url)).toBe(true)
  }
  expect(outside.aborted.filter((url) => !allowed.has(url))).toEqual([])
})

async function open(
  page: Page,
  context: BrowserContext,
  options: FakeOptions | null = {},
  story: WorldResponse = world1988WithYoutube,
) {
  if (options) await page.addInitScript(installFakeYT, options)
  outside = await interceptNetwork(context, story, { videos: 'hold' })
  await openDemo(page, DEMO_1988)
  await expect(page.locator('iframe')).toHaveCount(
    story === world1988WithYoutube ? 2 : 1,
  )
}

// For (d) and (j), which watch a pause caused by leaving the view: event ①'s
// Commons video and event ②'s player are taken out, so moving Daehan News
// never brings another player into view (in world1988WithYoutube the Commons
// video right above it enters as it moves down, and its takeover would pause
// Daehan News too, hiding whether leaving did). The day_media video stays,
// far below.
const [opening, kbs1, ...restEvents] = world1988WithYoutube.events
const daehanAlone: WorldResponse = {
  ...world1988WithYoutube,
  events: [
    {
      ...opening,
      media: opening.media.filter((m) => m.display_mode !== 'video_file'),
    },
    {
      ...kbs1,
      media: kbs1.media.filter((m) => m.display_mode !== 'video_embed'),
    },
    ...restEvents,
  ],
}
const dayVideo = (page: Page) => page.getByLabel('기록 영상', { exact: true })

test.describe('with the fake YT', () => {
  test.beforeEach(async ({ context, page }) => {
    await open(page, context)
  })

  test('(a) every player is at least 200×200 and (c) nothing scrolls sideways', async ({
    page,
  }) => {
    for (const iframe of [daehan(page), undated(page)]) {
      await toCenter(iframe)
      const box = await iframe.boundingBox()
      expect(box).not.toBeNull()
      expect(box!.width).toBeGreaterThanOrEqual(200)
      expect(box!.height).toBeGreaterThanOrEqual(200)
    }
    const { scrollWidth, clientWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }))
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth)
  })

  test('(b) nothing lies over the player, centre or corners', async ({
    page,
  }) => {
    for (const iframe of [daehan(page), undated(page)]) {
      await toCenter(iframe)
      const hits = await iframe.evaluate((element) => {
        const box = element.getBoundingClientRect()
        const points = [
          [box.left + box.width / 2, box.top + box.height / 2],
          [box.left + 2, box.top + 2],
          [box.right - 2, box.top + 2],
          [box.left + 2, box.bottom - 2],
          [box.right - 2, box.bottom - 2],
        ]
        return points.map(
          ([x, y]) => document.elementFromPoint(x, y) === element,
        )
      })
      expect(hits).toEqual([true, true, true, true, true])
    }
  })

  test('the iframe is the server embed URL on the nocookie player, Referer origin only', async ({
    page,
  }) => {
    for (const [iframe, url] of [
      [daehan(page), exampleDaehanNews.embed_url],
      [undated(page), exampleYoutubeUndated.embed_url],
    ] as const) {
      expect(await iframe.getAttribute('src')).toBe(url)
      expect(new URL(url).origin).toBe(YOUTUBE_EMBED_ORIGIN)
      expect(await iframe.getAttribute('referrerpolicy')).toBe(
        'strict-origin-when-cross-origin',
      )
      // Lazy iframes ask once near the screen; the request is stopped.
      await toCenter(iframe)
      await expect
        .poll(() => youtubeRequests.filter((r) => r.url === url).length)
        .toBeGreaterThanOrEqual(1)
    }
    // Only the two embed URLs went towards YouTube: no API script, no
    // www.youtube.com player, and only our origin as Referer.
    expect(new Set(youtubeRequests.map((r) => r.url))).toEqual(
      new Set(EMBED_URLS),
    )
    await expect
      .poll(() => youtubeRequests.every((r) => r.referer !== null))
      .toBe(true)
    for (const { referer } of youtubeRequests) {
      expect(referer).toBe('http://localhost:5173/')
    }
  })

  test('(e) never two playing: Commons video and YouTube, three turns', async ({
    page,
  }) => {
    const video = commonsVideo(page)
    const youtube = daehan(page)
    await expect(video).toHaveCount(1)

    // Each turn shows one alone (see showAlone).
    for (let round = 1; round <= 3; round++) {
      await showAlone(video)
      expect(await visibleRatio(youtube)).toBeLessThanOrEqual(0.5)
      await expect.poll(() => isPaused(video)).toBe(false)
      await expect.poll(() => ytPlayingNow(page)).toBe(1)

      await showAlone(youtube)
      expect(await visibleRatio(video)).toBeLessThanOrEqual(0.5)
      await expect.poll(() => ytState(youtube)).toBe(PLAYING)
      await expect.poll(() => isPaused(video)).toBe(true)
      await expect.poll(() => ytPlayingNow(page)).toBe(1)
      expect(await ytCount(youtube, 'playVideo')).toBe(round)
    }
    expect(await ytMaxPlaying(page)).toBe(1)
  })

  test('(g) error 150 hides that card and caption, the event stays', async ({
    page,
  }) => {
    const iframe = daehan(page)
    await toCenter(iframe)
    await expect.poll(async () => (await ytRecord(iframe))?.ready).toBe(true)
    await ytError(iframe, 150)

    await expect(daehan(page)).toHaveCount(0)
    await expect(page.getByText(exampleDaehanNews.attribution)).toHaveCount(0)
    // The other YouTube card is still there.
    await expect(undated(page)).toHaveCount(1)

    const first = world(page)
      .getByRole('list', { name: '사건 목록' })
      .getByRole('listitem')
      .first()
    await expect(first).toContainText('제24회 서울 하계 올림픽 개막.')
    await expect(first.getByRole('img')).toHaveCount(2)
    await expect(first.locator('video')).toHaveCount(1)
    await expect(first.getByRole('figure')).toHaveCount(3)
    await expect(first).toContainText('출처: 한국어 위키백과')
    await expect(world(page)).not.toContainText(ERROR_TEXT)
  })

  test('(h) the API script is never asked for when YT is there', async ({
    page,
  }) => {
    for (const iframe of [daehan(page), undated(page)]) await toCenter(iframe)
    await page.waitForTimeout(300)
    expect(youtubeRequests.filter((r) => r.url === IFRAME_API)).toEqual([])
    expect(await page.locator(`script[src="${IFRAME_API}"]`).count()).toBe(0)
  })

  test('(i) one YT.Player per iframe under the StrictMode dev server', async ({
    page,
  }) => {
    for (const iframe of [daehan(page), undated(page)]) {
      await toCenter(iframe)
      await expect.poll(async () => (await ytRecord(iframe))?.ready).toBe(true)
      expect(await ytCreatedFor(iframe)).toBe(1)
    }
    // Never destroyed (C11).
    for (const iframe of [daehan(page), undated(page)]) {
      expect((await ytRecord(iframe))?.calls).not.toContain('destroy')
    }
  })
})

test.describe('Daehan News as the only player near it', () => {
  test.beforeEach(async ({ context, page }) => {
    await open(page, context, {}, daehanAlone)
  })

  test('(d) plays Daehan News muted in view, pauses at half or less', async ({
    page,
  }) => {
    const iframe = daehan(page)
    await toCenter(iframe)
    expect(await visibleRatio(iframe)).toBeGreaterThan(0.5)
    await expect.poll(() => ytPlayback(iframe)).toEqual(['mute', 'playVideo'])
    await expect.poll(() => ytState(iframe)).toBe(PLAYING)

    await showTopShare(iframe, 0.4)
    expect(await visibleRatio(iframe)).toBeLessThanOrEqual(0.5)
    await expect
      .poll(() => ytPlayback(iframe))
      .toEqual(['mute', 'playVideo', 'pauseVideo'])
    // Paused because it left: no other player was ever started.
    expect(await page.locator('iframe').count()).toBe(1)
    expect(await isPaused(dayVideo(page))).toBe(true)
    expect(await ytMaxPlaying(page)).toBe(1)
  })

  test('(j) a play started under half visible pauses once off screen', async ({
    page,
  }) => {
    const iframe = daehan(page)
    await showTopShare(iframe, 0.3)
    const ratio = await visibleRatio(iframe)
    expect(ratio).toBeGreaterThan(0.05)
    expect(ratio).toBeLessThanOrEqual(0.5)
    await settle(page, iframe)
    await expect.poll(async () => (await ytRecord(iframe))?.ready).toBe(true)
    expect(await ytCount(iframe, 'playVideo')).toBe(0)

    await ytUserPlay(iframe)
    await expect.poll(() => ytState(iframe)).toBe(PLAYING)
    // Still under half: nothing pauses it yet.
    await page.waitForTimeout(300)
    expect(await ytCount(iframe, 'pauseVideo')).toBe(0)

    await pushBelowView(iframe)
    expect(await visibleRatio(iframe)).toBe(0)
    await expect.poll(() => ytCount(iframe, 'pauseVideo')).toBe(1)
    // Paused by leaving the screen: no other player was ever started.
    expect(await isPaused(dayVideo(page))).toBe(true)
    expect(await ytMaxPlaying(page)).toBe(1)
  })

  // T4 rerun 2: the reader taps play under half visible and scrolls it off
  // screen before the player's PLAYING arrives (the reverse of (j)).
  test('a user play reported after it left the screen is still paused', async ({
    page,
  }) => {
    const iframe = daehan(page)
    await showTopShare(iframe, 0.3)
    await settle(page, iframe)
    await expect.poll(async () => (await ytRecord(iframe))?.ready).toBe(true)
    await ytHold(page)
    await ytUserPlay(iframe)
    await pushBelowView(iframe)
    expect(await visibleRatio(iframe)).toBe(0)
    await settle(page, iframe)
    await ytResume(page)
    await page.waitForTimeout(400)
    expect(await ytQueued(iframe)).toEqual([])
    // It must not play off screen.
    expect(await ytCount(iframe, 'pauseVideo')).toBe(1)
    expect((await ytRecord(iframe))?.actual).not.toBe(PLAYING)
  })
})

test.describe('the reader asks for less motion', () => {
  test('(f) reduced motion: no mute, no playVideo', async ({
    context,
    page,
  }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await open(page, context)
    for (const iframe of [daehan(page), undated(page)]) {
      await toCenter(iframe)
      await settle(page, iframe, 0.5)
      await expect.poll(async () => (await ytRecord(iframe))?.ready).toBe(true)
      await page.waitForTimeout(200)
      expect(await ytPlayback(iframe)).toEqual([])
    }
  })
})

// Found in T4, beyond T3 (7): away, back and away again before the first
// start is reported (quick scrolling). The fake holds every report until
// the test lets it through, so the order is fixed: the first start's two
// reports arrive after our second playVideo() and before our next pause.
// Correct in both fake modes: the player that left stays paused, the video
// in view keeps playing, and it autoplays again when it comes back.
for (const mode of ['none', 'paused'] as const) {
  test.describe(`a start cancelled before its report (fake mode ${mode})`, () => {
    test('away, back and away: the video in view keeps playing', async ({
      context,
      page,
    }) => {
      await open(page, context, { pauseBeforeStart: mode })
      const video = commonsVideo(page)
      const youtube = daehan(page)
      await showAlone(video)
      await expect.poll(() => isPaused(video)).toBe(false)
      await expect.poll(async () => (await ytRecord(youtube))?.ready).toBe(true)
      await ytHold(page)

      await showAlone(youtube)
      await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(1)
      await expect.poll(() => isPaused(video)).toBe(true)
      await showAlone(video)
      await expect.poll(() => ytCount(youtube, 'pauseVideo')).toBe(1)
      await expect.poll(() => isPaused(video)).toBe(false)
      await showAlone(youtube)
      await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(2)
      await expect.poll(() => isPaused(video)).toBe(true)

      // 'none': BUFFERING, PLAYING (the start still came);
      // 'paused': BUFFERING, PAUSED (the start was cancelled).
      await ytDeliver(youtube, 2)
      await showAlone(video)
      await expect.poll(() => isPaused(video)).toBe(false)
      await ytResume(page)
      await page.waitForTimeout(500)

      expect(await ytQueued(youtube)).toEqual([])
      expect(await isPaused(video)).toBe(false)
      expect((await ytRecord(youtube))?.actual).not.toBe(PLAYING)
      expect(await ytMaxPlaying(page)).toBe(1)
      // None of it was the user's pause: it autoplays once back.
      const plays = await ytCount(youtube, 'playVideo')
      await showAlone(youtube)
      await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(plays + 1)
      await expect.poll(() => ytState(youtube)).toBe(PLAYING)
      expect(await ytMaxPlaying(page)).toBe(1)
    })
  })
}

// T4 rerun, probing the fix (22fac92): the same away, back, away, with a
// different number of the player's reports let through before the second
// "away" (0: none yet; 1: the first BUFFERING, so our pause meets a
// buffering player; 3: as many as there are, up to the second BUFFERING).
// The result must not depend on it.
for (const mode of ['none', 'paused'] as const) {
  for (const early of [0, 1, 3]) {
    test.describe(`quick turns, fake mode ${mode}, ${early} report(s) early`, () => {
      test('the video in view keeps playing and the player autoplays later', async ({
        context,
        page,
      }) => {
        await open(page, context, { pauseBeforeStart: mode })
        const video = commonsVideo(page)
        const youtube = daehan(page)
        await showAlone(video)
        await expect.poll(() => isPaused(video)).toBe(false)
        await expect
          .poll(async () => (await ytRecord(youtube))?.ready)
          .toBe(true)
        await ytHold(page)

        await showAlone(youtube)
        await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(1)
        await showAlone(video)
        await expect.poll(() => ytCount(youtube, 'pauseVideo')).toBe(1)
        await expect.poll(() => isPaused(video)).toBe(false)
        await showAlone(youtube)
        await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(2)
        await expect.poll(() => isPaused(video)).toBe(true)

        const queued = (await ytQueued(youtube)).length
        await ytDeliver(youtube, Math.min(early, queued))
        await showAlone(video)
        await expect.poll(() => isPaused(video)).toBe(false)
        await ytResume(page)
        await page.waitForTimeout(500)

        expect(await ytQueued(youtube)).toEqual([])
        expect(await isPaused(video)).toBe(false)
        expect((await ytRecord(youtube))?.actual).not.toBe(PLAYING)
        expect(await ytMaxPlaying(page)).toBe(1)
        const plays = await ytCount(youtube, 'playVideo')
        await showAlone(youtube)
        await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(plays + 1)
        await expect.poll(() => ytState(youtube)).toBe(PLAYING)
        await expect.poll(() => isPaused(video)).toBe(true)
        expect(await ytMaxPlaying(page)).toBe(1)
      })
    })
  }
}

// T4 rerun 2 (9a7d037): three away-and-back turns while the reports are
// held, `early` of them let through after every play, ending away (the
// video alone in view) or back (the player alone in view). Whatever the
// order, one plays at the end, and the player still autoplays later.
for (const mode of ['none', 'paused'] as const) {
  for (const early of [0, 1, 2]) {
    for (const end of ['away', 'back'] as const) {
      test.describe(`three quick turns, fake mode ${mode}, ${early} early, ending ${end}`, () => {
        test('one plays at the end and nothing is read as the user’s', async ({
          context,
          page,
        }) => {
          await open(page, context, { pauseBeforeStart: mode })
          const video = commonsVideo(page)
          const youtube = daehan(page)
          await showAlone(video)
          await expect.poll(() => isPaused(video)).toBe(false)
          await expect
            .poll(async () => (await ytRecord(youtube))?.ready)
            .toBe(true)
          await ytHold(page)

          for (let turn = 1; turn <= 3; turn++) {
            await showAlone(youtube)
            await expect.poll(() => isPaused(video)).toBe(true)
            const queued = (await ytQueued(youtube)).length
            await ytDeliver(youtube, Math.min(early, queued))
            if (turn === 3 && end === 'back') break
            await showAlone(video)
            await expect.poll(() => isPaused(video)).toBe(false)
          }
          await ytResume(page)
          await page.waitForTimeout(600)
          expect(await ytQueued(youtube)).toEqual([])

          const youtubePlays = (await ytRecord(youtube))?.actual === PLAYING
          expect(youtubePlays).toBe(end === 'back')
          expect(await isPaused(video)).toBe(end === 'back')
          expect(await ytMaxPlaying(page)).toBe(1)

          // Not the user's pause: away and back once more, it autoplays.
          if (end === 'back') {
            await showAlone(video)
            await expect.poll(() => isPaused(video)).toBe(false)
          }
          const plays = await ytCount(youtube, 'playVideo')
          await showAlone(youtube)
          await expect.poll(() => ytCount(youtube, 'playVideo')).toBe(plays + 1)
          await expect.poll(() => ytState(youtube)).toBe(PLAYING)
          await expect.poll(() => isPaused(video)).toBe(true)
          expect(await ytMaxPlaying(page)).toBe(1)
        })
      })
    }
  }
}

test.describe('without the fake (decision 1 (a))', () => {
  test('(h) the API script is asked for once, stopped, and the players stay', async ({
    context,
    page,
  }) => {
    alsoAborted = [IFRAME_API]
    await open(page, context, null)
    for (const iframe of [daehan(page), undated(page)]) await toCenter(iframe)
    await expect
      .poll(() => outside.aborted.filter((url) => url === IFRAME_API).length)
      .toBe(1)
    await page.waitForTimeout(300)
    expect(youtubeRequests.filter((r) => r.url === IFRAME_API)).toHaveLength(1)
    await expect(page.locator(`script[src="${IFRAME_API}"]`)).toHaveCount(1)
    expect(await page.evaluate(() => 'YT' in window)).toBe(false)
    // Both official players stay, inside their cards, with no error text.
    for (const iframe of [daehan(page), undated(page)]) {
      await expect(iframe).toHaveCount(1)
      await expect(iframe.locator('xpath=..')).toHaveJSProperty(
        'tagName',
        'FIGURE',
      )
    }
    await expect(world(page)).not.toContainText(ERROR_TEXT)
  })
})

// Demo table rows 2002-06-22 and 1995-03-15, without the fake: a story with
// no YouTube record never loads the API script.
test.describe('days without YouTube', () => {
  for (const [label, story] of [
    ['2002년 6월 22일로 보기', world2002WithMedia],
    ['1995년 3월 15일로 보기', world1995],
  ] as const) {
    test(`${label}: no iframe, no API script, no YouTube request`, async ({
      context,
      page,
    }) => {
      outside = await interceptNetwork(context, story)
      await openDemo(page, label)
      await page.waitForTimeout(300)
      await expect(page.locator('iframe')).toHaveCount(0)
      await expect(page.locator('script[src*="iframe_api"]')).toHaveCount(0)
      expect(youtubeRequests).toEqual([])
      await expect(world(page)).not.toContainText(ERROR_TEXT)
    })
  }
})

test.describe('before consent (plan T4 (3), 10.1 #23)', () => {
  test('no YouTube request, iframe or script on the input screen', async ({
    context,
    page,
  }) => {
    await page.addInitScript(installFakeYT, {})
    outside = await interceptNetwork(context, world1988WithYoutube, {
      videos: 'hold',
    })
    await page.goto('/')
    await expect(
      page.getByRole('checkbox', { name: CONSENT_LABEL }),
    ).not.toBeChecked()
    await page.getByRole('button', { name: DEMO_1988 }).click({ force: true })
    await page.waitForTimeout(500)
    await expect(world(page)).toHaveCount(0)
    await expect(page.locator('iframe')).toHaveCount(0)
    await expect(page.locator('script[src*="iframe_api"]')).toHaveCount(0)
    expect(youtubeRequests).toEqual([])
    expect(outside.aborted).toEqual([])
  })
})
