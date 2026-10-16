import type { Locator, Page } from '@playwright/test'

// A synthetic `window.YT` for the e2e tests (plan M3b3c T4), built from the
// field structure of the YouTube IFrame Player API reference, like the unit
// fake in src/test/youtubeFake.ts (CLAUDE.md 7.5): no real script, response
// or video id. Installed with `page.addInitScript(installFakeYT, options)`
// before any app code, so the app finds `YT.Player` and never inserts the
// API script.
//
// Same meaning as the unit fake: `playVideo()` reports BUFFERING then
// PLAYING; `pauseVideo()` while playing or buffering reports PAUSED; before
// any start it reports nothing. A pause after `playVideo()` but before the
// start is reported: 'none' (default) sends nothing and the start still
// comes (the worse case), 'paused' cancels the start (no PLAYING) and sends
// only PAUSED. Unlike jsdom, reports arrive late here, one by one, as from a
// real player in another frame.
//
// Everything a test needs is on `window.__yt`. Each player is found by its
// iframe element, so a test passes a locator (see the helpers below).

export type PauseBeforeStart = 'none' | 'paused'
export type FakeOptions = { pauseBeforeStart?: PauseBeforeStart }

type Events = {
  onReady?(event: { target: unknown }): void
  onStateChange?(event: { target: unknown; data: number }): void
  onError?(event: { target: unknown; data: number }): void
  onAutoplayBlocked?(event: { target: unknown }): void
}

export type FakeRecord = {
  calls: string[]
  // Every state reported to the page, in order.
  reports: number[]
  // Last reported state, and what the player is really doing now (a pause
  // stops it at once; its report comes later).
  reported: number
  actual: number
  ready: boolean
  muted: boolean
}

export type FakeHub = {
  // `YT.Player` objects made for this element (StrictMode check).
  createdFor(element: Element): number
  record(element: Element): FakeRecord | null
  // As if the reader pressed play in the player's own controls.
  userPlay(element: Element): void
  error(element: Element, code: number): void
  // Most players ever playing at once: YouTube players whose `actual` is
  // PLAYING plus <video> elements with `paused === false`.
  maxPlaying: number
  playingNow(): number
  sample(): void
  // Holds every report back until `deliver` (that many, now, in order) or
  // `resume` (all again, one by one): the order of our calls and of the
  // player's reports is then up to the test.
  hold(): void
  deliver(element: Element, count: number): void
  resume(): void
  queued(element: Element): number[]
}

// Runs in the page. Must stay self-contained (serialized by Playwright).
export function installFakeYT(options: FakeOptions = {}) {
  const pauseBeforeStart = options.pauseBeforeStart ?? 'none'
  const UNSTARTED = -1
  const PLAYING = 1
  const PAUSED = 2
  const BUFFERING = 3
  // Delay before the player is ready and between two reports.
  const READY_MS = 50
  const REPORT_MS = 40
  const running = (state: number) => state === PLAYING || state === BUFFERING

  type Entry = { data: number }
  type Internal = FakeRecord & {
    events: Events
    target: number
    queue: Entry[]
    timer: number | null
    self: unknown
  }
  const byElement = new Map<Element, Internal[]>()
  const all: Internal[] = []
  let holding = false
  const playerOf = (element: Element) => {
    const p = byElement.get(element)?.[0]
    if (!p) throw new Error('no player for this element')
    return p
  }

  const hub: FakeHub = {
    createdFor: (element) => byElement.get(element)?.length ?? 0,
    record: (element) => byElement.get(element)?.[0] ?? null,
    userPlay(element) {
      const p = playerOf(element)
      p.target = PLAYING
      send(p, PLAYING)
    },
    error(element, code) {
      const p = playerOf(element)
      setTimeout(() => p.events.onError?.({ target: p.self, data: code }), 0)
    },
    maxPlaying: 0,
    playingNow: () =>
      all.filter((p) => p.actual === PLAYING).length +
      [...document.querySelectorAll('video')].filter((v) => !v.paused).length,
    sample() {
      hub.maxPlaying = Math.max(hub.maxPlaying, hub.playingNow())
    },
    hold() {
      holding = true
    },
    deliver(element, count) {
      const p = playerOf(element)
      for (let i = 0; i < count; i++) {
        if (!dispatch(p)) throw new Error('no report left to deliver')
      }
    },
    resume() {
      holding = false
      all.forEach(pump)
    },
    queued: (element) => playerOf(element).queue.map((entry) => entry.data),
  }
  ;(window as unknown as { __yt: FakeHub }).__yt = hub

  // Sends the player's next report; false when there is none.
  function dispatch(p: Internal) {
    const next = p.queue.shift()
    if (!next) return false
    p.reported = next.data
    p.reports.push(next.data)
    // What the player does follows where it was last sent: a report of a
    // start we already paused does not start it again.
    if (running(next.data) ? running(p.target) : !running(p.target)) {
      p.actual = next.data
    }
    p.events.onStateChange?.({ target: p.self, data: next.data })
    hub.sample()
    return true
  }
  // Reports go out one at a time, in order, each a little later.
  function pump(p: Internal) {
    if (p.timer !== null || holding) return
    p.timer = window.setTimeout(() => {
      p.timer = null
      if (holding) return
      if (dispatch(p)) pump(p)
    }, REPORT_MS)
  }
  function send(p: Internal, data: number) {
    p.queue.push({ data })
    pump(p)
  }

  class Player {
    constructor(element: HTMLIFrameElement, opts: { events: Events }) {
      const p: Internal = {
        calls: [],
        reports: [],
        reported: UNSTARTED,
        actual: UNSTARTED,
        ready: false,
        muted: false,
        events: opts.events,
        target: UNSTARTED,
        queue: [],
        timer: null,
        self: this,
      }
      all.push(p)
      byElement.set(element, [...(byElement.get(element) ?? []), p])
      const call = (method: string) => {
        if (!p.ready) throw new TypeError(`${method} called before onReady`)
        p.calls.push(method)
      }
      const self = this as unknown as Record<string, unknown>
      self.mute = () => {
        call('mute')
        p.muted = true
      }
      self.unMute = () => {
        call('unMute')
        p.muted = false
      }
      self.isMuted = () => {
        call('isMuted')
        return p.muted
      }
      self.getPlayerState = () => {
        call('getPlayerState')
        return p.reported
      }
      self.playVideo = () => {
        call('playVideo')
        if (running(p.target)) return
        p.target = PLAYING
        send(p, BUFFERING)
        send(p, PLAYING)
      }
      self.pauseVideo = () => {
        call('pauseVideo')
        if (running(p.reported)) {
          // Stops now; the report comes later.
          p.actual = PAUSED
          p.target = PAUSED
          send(p, PAUSED)
        } else if (running(p.target) && pauseBeforeStart === 'paused') {
          const at = p.queue.findLastIndex((e) => e.data === PLAYING)
          if (at >= 0) p.queue.splice(at, 1)
          p.actual = PAUSED
          p.target = PAUSED
          send(p, PAUSED)
        }
        hub.sample()
      }
      // Recorded so a test can see it is never called (plan C11).
      self.destroy = () => p.calls.push('destroy')
      setTimeout(() => {
        p.ready = true
        p.events.onReady?.({ target: this })
      }, READY_MS)
    }
  }

  ;(window as unknown as { YT: unknown }).YT = {
    Player,
    PlayerState: {
      UNSTARTED,
      ENDED: 0,
      PLAYING,
      PAUSED,
      BUFFERING,
      CUED: 5,
    },
  }

  for (const type of ['play', 'playing', 'pause']) {
    document.addEventListener(type, () => hub.sample(), true)
  }
  const tick = () => {
    hub.sample()
    requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

type Win = { __yt: FakeHub }

export const ytRecord = (iframe: Locator) =>
  iframe.evaluate((el) => (window as unknown as Win).__yt.record(el))

// The playback calls our code made on the player, in order.
export const ytPlayback = async (iframe: Locator) =>
  ((await ytRecord(iframe))?.calls ?? []).filter((method) =>
    ['mute', 'playVideo', 'pauseVideo'].includes(method),
  )

export const ytCount = async (iframe: Locator, method: string) =>
  (await ytPlayback(iframe)).filter((m) => m === method).length

export const ytCreatedFor = (iframe: Locator) =>
  iframe.evaluate((el) => (window as unknown as Win).__yt.createdFor(el))

export const ytUserPlay = (iframe: Locator) =>
  iframe.evaluate((el) => (window as unknown as Win).__yt.userPlay(el))

export const ytError = (iframe: Locator, code: number) =>
  iframe.evaluate(
    (el, value) => (window as unknown as Win).__yt.error(el, value),
    code,
  )

export const ytHold = (page: Page) =>
  page.evaluate(() => (window as unknown as Win).__yt.hold())

export const ytDeliver = (iframe: Locator, count: number) =>
  iframe.evaluate(
    (el, n) => (window as unknown as Win).__yt.deliver(el, n),
    count,
  )

export const ytResume = (page: Page) =>
  page.evaluate(() => (window as unknown as Win).__yt.resume())

export const ytQueued = (iframe: Locator) =>
  iframe.evaluate((el) => (window as unknown as Win).__yt.queued(el))

export const ytMaxPlaying = (page: Page) =>
  page.evaluate(() => (window as unknown as Win).__yt.maxPlaying)

export const ytPlayingNow = (page: Page) =>
  page.evaluate(() => (window as unknown as Win).__yt.playingNow())
