import { describe, expect, it, vi } from 'vitest'
import { createPlaybackCoordinator } from './playbackCoordinator'

const player = () => ({
  play: vi.fn<() => void>(),
  pause: vi.fn<() => void>(),
})

// Every test makes its own coordinator: no state is shared between tests.
describe('createPlaybackCoordinator', () => {
  it('keeps one player playing at a time', () => {
    const coordinator = createPlaybackCoordinator()
    const [a, b, c] = [player(), player(), player()]
    for (const p of [a, b, c]) coordinator.register(p)

    coordinator.requestPlay(a)
    expect(a.play).toHaveBeenCalledTimes(1)
    expect(a.pause).not.toHaveBeenCalled()

    coordinator.requestPlay(b)
    expect(a.pause).toHaveBeenCalledTimes(1)
    expect(b.play).toHaveBeenCalledTimes(1)

    coordinator.notifyPlaying(c)
    expect(b.pause).toHaveBeenCalledTimes(1)
    expect(c.play).not.toHaveBeenCalled()

    coordinator.release(c)
    expect(c.pause).toHaveBeenCalledTimes(1)
    expect(a.play).toHaveBeenCalledTimes(1)
    expect(a.pause).toHaveBeenCalledTimes(1)
    expect(b.pause).toHaveBeenCalledTimes(1)
  })

  it('does not pause a released player that is not playing', () => {
    const coordinator = createPlaybackCoordinator()
    const [a, b] = [player(), player()]
    coordinator.register(a)
    coordinator.register(b)
    coordinator.requestPlay(a)
    coordinator.release(b)
    expect(b.pause).not.toHaveBeenCalled()
    expect(a.pause).not.toHaveBeenCalled()
    // After a release nothing is playing, so the next play pauses no one.
    coordinator.release(a)
    coordinator.requestPlay(b)
    expect(a.pause).toHaveBeenCalledTimes(1)
  })

  it('never calls a player again after it is unregistered', () => {
    const coordinator = createPlaybackCoordinator()
    const [a, b] = [player(), player()]
    const unregisterA = coordinator.register(a)
    coordinator.register(b)
    coordinator.requestPlay(a)
    unregisterA()

    coordinator.requestPlay(b)
    coordinator.notifyPlaying(b)
    coordinator.requestPlay(a)
    coordinator.notifyPlaying(a)
    coordinator.release(a)

    expect(a.play).toHaveBeenCalledTimes(1)
    expect(a.pause).not.toHaveBeenCalled()
    expect(b.play).toHaveBeenCalledTimes(1)
    expect(b.pause).not.toHaveBeenCalled()
  })

  it('accepts a release with no players at all', () => {
    const coordinator = createPlaybackCoordinator()
    expect(() => coordinator.release(player())).not.toThrow()
  })
})
