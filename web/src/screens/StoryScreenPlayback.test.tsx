import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createPlaybackCoordinator } from '../media/playbackCoordinator'
import { StoryScreen } from './StoryScreen'

// Counts the coordinators made, keeping the real ones.
vi.mock('../media/playbackCoordinator', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../media/playbackCoordinator')>()
  return {
    ...actual,
    createPlaybackCoordinator: vi.fn(actual.createPlaybackCoordinator),
  }
})

const NO_EVENTS = '이 날짜에 기록된 큰 사건을 찾지 못했습니다.'

describe('StoryScreen playback coordinator (M3b3 T1 (4))', () => {
  it('makes one coordinator per result screen', async () => {
    const create = vi.mocked(createPlaybackCoordinator)
    create.mockClear()

    const { rerender, unmount } = render(
      <StoryScreen date="1995-03-15" onRestart={() => {}} />,
    )
    await screen.findByText(NO_EVENTS)
    expect(create).toHaveBeenCalledTimes(1)
    rerender(<StoryScreen date="1995-03-15" onRestart={() => {}} />)
    expect(create).toHaveBeenCalledTimes(1)
    unmount()

    // A new result (after "처음으로") is a new screen with its own coordinator.
    render(<StoryScreen date="1995-03-15" onRestart={() => {}} />)
    await screen.findByText(NO_EVENTS)
    expect(create).toHaveBeenCalledTimes(2)
    const [first, second] = create.mock.results.map((result) => result.value)
    expect(first).not.toBe(second)
  })
})
