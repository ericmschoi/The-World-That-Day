import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  usePlaybackCoordinator,
  type PlaybackCoordinator,
} from './playbackCoordinator'
import { PlaybackProvider } from './PlaybackProvider'

function Probe({ seen }: { seen: PlaybackCoordinator[] }) {
  seen.push(usePlaybackCoordinator())
  return null
}

describe('PlaybackProvider', () => {
  it('gives every player inside the same coordinator across renders', () => {
    const seen: PlaybackCoordinator[] = []
    // A new element each time, so React renders the probes again.
    const tree = () => (
      <PlaybackProvider>
        <Probe seen={seen} />
        <Probe seen={seen} />
      </PlaybackProvider>
    )
    const { rerender } = render(tree())
    rerender(tree())
    expect(seen).toHaveLength(4)
    expect(new Set(seen).size).toBe(1)
  })

  it('gives separate providers separate coordinators', () => {
    const first: PlaybackCoordinator[] = []
    const second: PlaybackCoordinator[] = []
    render(
      <>
        <PlaybackProvider>
          <Probe seen={first} />
        </PlaybackProvider>
        <PlaybackProvider>
          <Probe seen={second} />
        </PlaybackProvider>
      </>,
    )
    expect(first[0]).not.toBe(second[0])
  })

  it('gives a player outside any provider a coordinator of its own', () => {
    const seen: PlaybackCoordinator[] = []
    const { rerender } = render(
      <>
        <Probe seen={seen} />
        <Probe seen={seen} />
      </>,
    )
    expect(seen[0]).not.toBe(seen[1])
    rerender(
      <>
        <Probe seen={seen} />
        <Probe seen={seen} />
      </>,
    )
    expect(seen[2]).toBe(seen[0])
    expect(seen[3]).toBe(seen[1])
  })
})
