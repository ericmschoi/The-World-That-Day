import { afterEach, describe, expect, it } from 'vitest'
import { canAutoplay } from './autoplayPolicy'

// jsdom has neither `matchMedia` nor `navigator.connection`; each test sets
// what it needs on the instance and removes it again.
function setReducedMotion(reduce: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: (query: string) => ({
      matches: query === '(prefers-reduced-motion: reduce)' && reduce,
      media: query,
    }),
  })
}

function setConnection(value: unknown) {
  Object.defineProperty(navigator, 'connection', { configurable: true, value })
}

afterEach(() => {
  Reflect.deleteProperty(window, 'matchMedia')
  Reflect.deleteProperty(navigator, 'connection')
})

describe('canAutoplay', () => {
  it('allows autoplay without reduced motion or navigator.connection', () => {
    setReducedMotion(false)
    expect('connection' in navigator).toBe(false)
    expect(canAutoplay()).toBe(true)
  })

  it('allows autoplay when matchMedia is missing', () => {
    expect(canAutoplay()).toBe(true)
  })

  it('refuses autoplay when reduced motion is on', () => {
    setReducedMotion(true)
    expect(canAutoplay()).toBe(false)
  })

  it('refuses autoplay when data saving is on', () => {
    setReducedMotion(false)
    setConnection({ saveData: true })
    expect(canAutoplay()).toBe(false)
  })

  it('allows autoplay when data saving is off', () => {
    setReducedMotion(false)
    setConnection({ saveData: false })
    expect(canAutoplay()).toBe(true)
  })

  it('reads the settings again on every call', () => {
    setReducedMotion(false)
    expect(canAutoplay()).toBe(true)
    setReducedMotion(true)
    expect(canAutoplay()).toBe(false)
    setReducedMotion(false)
    expect(canAutoplay()).toBe(true)
    setConnection({ saveData: true })
    expect(canAutoplay()).toBe(false)
  })
})
