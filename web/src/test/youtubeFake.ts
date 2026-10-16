import { act } from '@testing-library/react'
import { YOUTUBE_IFRAME_API_URL } from '../external/urls'
import type { YT, YTPlayer, YTPlayerEvents } from '../media/youtubeApi'

// A synthetic stand-in for `window.YT`, built from the field structure of the
// YouTube IFrame Player API reference (CLAUDE.md 7.5): no real responses or
// video ids. Install it in the test files that need it (never in setup.ts)
// and uninstall after each test, like mediaFakes.ts.

export const PlayerState = {
  UNSTARTED: -1,
  ENDED: 0,
  PLAYING: 1,
  PAUSED: 2,
  BUFFERING: 3,
  CUED: 5,
} as const

const { PLAYING, PAUSED, BUFFERING } = PlayerState
const running = (state: number) => state === PLAYING || state === BUFFERING

// What `pauseVideo()` does after `playVideo()` but before the start is
// reported. The reference does not say: 'none' sends nothing (the worse case,
// the default), 'paused' sends PAUSED.
export type PauseBeforeStart = 'none' | 'paused'

export type PlayerCall = { player: FakePlayer; method: string }

type Hub = {
  emit(player: FakePlayer, send: () => void): void
  record(player: FakePlayer, method: string): void
  pauseBeforeStart: PauseBeforeStart
}

export class FakePlayer implements YTPlayer {
  readonly element: HTMLIFrameElement
  readonly events: YTPlayerEvents
  ready = false
  muted = false
  // The last state reported to the page (what getPlayerState returns)...
  state: number = PlayerState.UNSTARTED
  // ...and the state after every event still queued.
  private target: number = PlayerState.UNSTARTED
  private readonly hub: Hub

  constructor(element: HTMLIFrameElement, events: YTPlayerEvents, hub: Hub) {
    this.element = element
    this.events = events
    this.hub = hub
  }

  // Like the real player, methods do not exist before `onReady`.
  private call(method: string) {
    if (!this.ready) throw new TypeError(`${method} called before onReady`)
    this.hub.record(this, method)
  }

  // Schedules a state change; `state` changes when the page hears it.
  sendState(data: number) {
    this.target = data
    this.hub.emit(this, () => {
      this.state = data
      this.events.onStateChange?.({ target: this, data })
    })
  }

  mute() {
    this.call('mute')
    this.muted = true
  }
  unMute() {
    this.call('unMute')
    this.muted = false
  }
  isMuted() {
    this.call('isMuted')
    return this.muted
  }
  getPlayerState() {
    this.call('getPlayerState')
    return this.state
  }
  playVideo() {
    this.call('playVideo')
    if (running(this.target)) return
    this.sendState(BUFFERING)
    this.sendState(PLAYING)
  }
  pauseVideo() {
    this.call('pauseVideo')
    if (running(this.state)) this.sendState(PAUSED)
    else if (running(this.target) && this.hub.pauseBeforeStart === 'paused') {
      this.sendState(PAUSED)
    }
  }
  // Recorded so a test can check it is never called (plan C11).
  destroy() {
    this.hub.record(this, 'destroy')
  }
}

export type YouTubeFake = {
  readonly YT: YT
  // Every player created since install, in creation order.
  readonly players: readonly FakePlayer[]
  playersFor(element: Element): FakePlayer[]
  // Every method call on any player, in call order.
  readonly calls: readonly PlayerCall[]
  callsOf(player: FakePlayer): string[]
  setPauseBeforeStart(mode: PauseBeforeStart): void
  // Senders, as the player would. Inside `act` unless events are deferred.
  ready(player: FakePlayer): void
  state(player: FakePlayer, data: number): void
  error(player: FakePlayer, code: number): void
  autoplayBlocked(player: FakePlayer): void
  // Calls `window.onYouTubeIframeAPIReady`, as the loaded script does.
  announce(): void
  // Queue events instead of sending them, as the real player does...
  deferEvents(): void
  // ...and send the queued ones in order (and any they cause), inside `act`.
  flushEvents(): void
  uninstall(): void
}

// The API script tags in the document, for counting.
export const youtubeScripts = () =>
  document.querySelectorAll(`script[src="${YOUTUBE_IFRAME_API_URL}"]`)

// The page as it was before any test: uninstall goes back to this, so a
// callback set by `loadYouTubeApi` in one test does not reach the next.
const own = (key: string) => Object.prototype.hasOwnProperty.call(window, key)
const pristine = {
  hadYT: own('YT'),
  YT: window.YT,
  hadReady: own('onYouTubeIframeAPIReady'),
  ready: window.onYouTubeIframeAPIReady,
}

export function installYouTubeFake(): YouTubeFake {
  const players: FakePlayer[] = []
  const calls: PlayerCall[] = []
  const queue: (() => void)[] = []
  let deferring = false

  const hub: Hub = {
    // Method calls come from our code, maybe already inside `act`: no `act`.
    emit(_player, send) {
      if (deferring) queue.push(send)
      else send()
    },
    record(player, method) {
      calls.push({ player, method })
    },
    pauseBeforeStart: 'none',
  }
  const sendNow = (send: () => void) => {
    if (deferring) queue.push(send)
    else act(send)
  }

  class Player extends FakePlayer {
    constructor(
      element: HTMLIFrameElement,
      options: { events: YTPlayerEvents },
    ) {
      super(element, options.events, hub)
      players.push(this)
    }
  }
  const YT: YT = { Player, PlayerState: { ...PlayerState } }
  window.YT = YT

  return {
    YT,
    players,
    playersFor: (element) => players.filter((p) => p.element === element),
    calls,
    callsOf: (player) =>
      calls.filter((c) => c.player === player).map((c) => c.method),
    setPauseBeforeStart(mode) {
      hub.pauseBeforeStart = mode
    },
    ready(player) {
      sendNow(() => {
        player.ready = true
        player.events.onReady?.({ target: player })
      })
    },
    state(player, data) {
      if (deferring) player.sendState(data)
      else act(() => player.sendState(data))
    },
    error(player, code) {
      sendNow(() => player.events.onError?.({ target: player, data: code }))
    },
    autoplayBlocked(player) {
      sendNow(() => player.events.onAutoplayBlocked?.({ target: player }))
    },
    announce() {
      act(() => window.onYouTubeIframeAPIReady?.())
    },
    deferEvents() {
      deferring = true
    },
    flushEvents() {
      act(() => {
        for (let next = queue.shift(); next; next = queue.shift()) next()
      })
    },
    uninstall() {
      queue.length = 0
      for (const script of youtubeScripts()) script.remove()
      if (pristine.hadYT) window.YT = pristine.YT
      else Reflect.deleteProperty(window, 'YT')
      if (pristine.hadReady) window.onYouTubeIframeAPIReady = pristine.ready
      else Reflect.deleteProperty(window, 'onYouTubeIframeAPIReady')
    },
  }
}
