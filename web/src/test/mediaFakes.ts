import { act } from '@testing-library/react'
import { vi, type Mock } from 'vitest'

// Test doubles for video tests. jsdom has no IntersectionObserver and only
// "Not implemented" stubs for media playback. Install them in the video test
// files that need them (never in setup.ts) and uninstall after each test.

type Callback = IntersectionObserverCallback

export class FakeIntersectionObserver {
  readonly targets = new Set<Element>()
  readonly thresholds: readonly number[]
  disconnected = false
  readonly callback: Callback

  constructor(callback: Callback, options?: IntersectionObserverInit) {
    this.callback = callback
    const threshold = options?.threshold ?? 0
    this.thresholds = Array.isArray(threshold) ? threshold : [threshold]
    observers.push(this)
  }
  observe(target: Element) {
    this.targets.add(target)
  }
  unobserve(target: Element) {
    this.targets.delete(target)
  }
  disconnect() {
    this.targets.clear()
    this.disconnected = true
  }
  takeRecords(): IntersectionObserverEntry[] {
    return []
  }
}

const observers: FakeIntersectionObserver[] = []

export type IntersectionObserverFake = {
  // Every observer created since install, in creation order.
  readonly observers: readonly FakeIntersectionObserver[]
  // Calls back every live observer that watches `target`, inside `act`.
  setRatio(target: Element, ratio: number): void
  uninstall(): void
}

export function installIntersectionObserverFake(): IntersectionObserverFake {
  const had = Object.prototype.hasOwnProperty.call(
    globalThis,
    'IntersectionObserver',
  )
  const previous = globalThis.IntersectionObserver
  observers.length = 0
  globalThis.IntersectionObserver =
    FakeIntersectionObserver as unknown as typeof IntersectionObserver
  return {
    observers,
    setRatio(target, ratio) {
      const rect = target.getBoundingClientRect()
      const entry = {
        target,
        intersectionRatio: ratio,
        isIntersecting: ratio > 0,
        time: performance.now(),
        boundingClientRect: rect,
        intersectionRect: rect,
        rootBounds: null,
      } as IntersectionObserverEntry
      act(() => {
        for (const observer of observers) {
          if (observer.targets.has(target)) {
            observer.callback(
              [entry],
              observer as unknown as IntersectionObserver,
            )
          }
        }
      })
    },
    uninstall() {
      observers.length = 0
      if (had) globalThis.IntersectionObserver = previous
      else Reflect.deleteProperty(globalThis, 'IntersectionObserver')
    },
  }
}

export type MediaElementFake = {
  // `this` of each call is in `play.mock.contexts`.
  play: Mock<(this: HTMLMediaElement) => Promise<void>>
  pause: Mock<(this: HTMLMediaElement) => void>
  // `muted` of the element at each `play()` call, in call order.
  readonly mutedAtPlay: boolean[]
  // From now on `play()` rejects with `error` (null: resolves again).
  rejectPlayWith(error: Error | null): void
  uninstall(): void
}

const MEDIA_KEYS = ['play', 'pause', 'load', 'paused'] as const

// `play()` resolves (or rejects, see `rejectPlayWith`), marks the element as
// playing and fires `play`; `pause()` on a playing element marks it paused and
// fires `pause`, like a browser. Events fire synchronously.
export function installMediaElementFake(): MediaElementFake {
  const proto = HTMLMediaElement.prototype
  const saved = MEDIA_KEYS.map(
    (key) => [key, Object.getOwnPropertyDescriptor(proto, key)] as const,
  )
  const playing = new WeakSet<HTMLMediaElement>()
  const mutedAtPlay: boolean[] = []
  let rejection: Error | null = null

  const play = vi.fn(function (this: HTMLMediaElement) {
    mutedAtPlay.push(this.muted)
    if (rejection !== null) return Promise.reject(rejection)
    if (!playing.has(this)) {
      playing.add(this)
      this.dispatchEvent(new Event('play'))
    }
    return Promise.resolve()
  })
  const pause = vi.fn(function (this: HTMLMediaElement) {
    if (playing.delete(this)) this.dispatchEvent(new Event('pause'))
  })
  Object.defineProperties(proto, {
    play: { configurable: true, writable: true, value: play },
    pause: { configurable: true, writable: true, value: pause },
    load: { configurable: true, writable: true, value: () => {} },
    paused: {
      configurable: true,
      get(this: HTMLMediaElement) {
        return !playing.has(this)
      },
    },
  })

  return {
    play,
    pause,
    mutedAtPlay,
    rejectPlayWith(error) {
      rejection = error
    },
    uninstall() {
      for (const [key, descriptor] of saved) {
        if (descriptor) Object.defineProperty(proto, key, descriptor)
        else Reflect.deleteProperty(proto, key)
      }
    },
  }
}
