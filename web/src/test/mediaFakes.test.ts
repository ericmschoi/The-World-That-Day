import { afterEach, describe, expect, it } from 'vitest'
import {
  installIntersectionObserverFake,
  installMediaElementFake,
  type IntersectionObserverFake,
  type MediaElementFake,
} from './mediaFakes'

let observerFake: IntersectionObserverFake | null = null
let mediaFake: MediaElementFake | null = null

afterEach(() => {
  observerFake?.uninstall()
  mediaFake?.uninstall()
  observerFake = mediaFake = null
})

describe('installIntersectionObserverFake', () => {
  it('calls back observers of the target with the given ratio', () => {
    observerFake = installIntersectionObserverFake()
    const target = document.createElement('div')
    const other = document.createElement('div')
    const ratios: number[] = []
    const observer = new IntersectionObserver(
      (entries) => ratios.push(...entries.map((e) => e.intersectionRatio)),
      { threshold: [0.5, 0.51] },
    )
    observer.observe(target)
    observerFake.setRatio(target, 0.51)
    observerFake.setRatio(other, 0.9)
    observer.disconnect()
    observerFake.setRatio(target, 0.2)
    expect(ratios).toEqual([0.51])
    expect(observerFake.observers[0].thresholds).toEqual([0.5, 0.51])
  })

  it('removes the global again on uninstall', () => {
    observerFake = installIntersectionObserverFake()
    expect(globalThis.IntersectionObserver).toBeDefined()
    observerFake.uninstall()
    expect(globalThis.IntersectionObserver).toBeUndefined()
  })
})

describe('installMediaElementFake', () => {
  it('plays and pauses with events, recording muted at each play', async () => {
    mediaFake = installMediaElementFake()
    const video = document.createElement('video')
    const events: string[] = []
    video.addEventListener('play', () => events.push('play'))
    video.addEventListener('pause', () => events.push('pause'))

    video.muted = true
    await expect(video.play()).resolves.toBeUndefined()
    expect(video.paused).toBe(false)
    video.pause()
    video.pause()
    expect(video.paused).toBe(true)

    expect(events).toEqual(['play', 'pause'])
    expect(mediaFake.mutedAtPlay).toEqual([true])
    expect(mediaFake.play.mock.contexts).toEqual([video])
    expect(mediaFake.pause).toHaveBeenCalledTimes(2)
  })

  it('rejects play when asked, without playing', async () => {
    mediaFake = installMediaElementFake()
    const video = document.createElement('video')
    const error = new DOMException('blocked', 'NotAllowedError')
    mediaFake.rejectPlayWith(error)
    await expect(video.play()).rejects.toBe(error)
    expect(video.paused).toBe(true)
    mediaFake.rejectPlayWith(null)
    await expect(video.play()).resolves.toBeUndefined()
  })

  it('restores the jsdom members on uninstall', () => {
    const proto = HTMLMediaElement.prototype
    const before = Object.getOwnPropertyDescriptor(proto, 'play')
    mediaFake = installMediaElementFake()
    expect(proto.play).toBe(mediaFake.play)
    mediaFake.uninstall()
    expect(Object.getOwnPropertyDescriptor(proto, 'play')).toEqual(before)
  })
})
