import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from './App'
import { world1988 } from './mocks/fixtures/world/responses'
import { server } from './mocks/server'

const DEMO_BUTTONS = [
  '1988년 9월 17일로 보기',
  '2002년 6월 22일로 보기',
  '1995년 3월 15일로 보기',
  // M3b3 T3 (6): the two temporary sample dates follow the demo dates.
  '표본: 1969년 7월 20일로 보기',
  '표본: 1986년 9월 20일로 보기',
]

describe('App', () => {
  it('switches between the input placeholder and the story', async () => {
    const user = userEvent.setup()
    render(<App />)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { level: 1, name: 'The World That Day' }),
    ).toBeInTheDocument()
    expect(screen.queryAllByRole('region')).toHaveLength(0)

    await user.click(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    )
    expect(screen.getAllByRole('region')).toHaveLength(5)
    await screen.findByRole('list', { name: '사건 목록' })

    await user.click(screen.getByRole('button', { name: '처음으로' }))
    expect(screen.queryAllByRole('region')).toHaveLength(0)
    expect(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    ).toBeInTheDocument()
  })

  it('works with the keyboard alone', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.tab()
    expect(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    ).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(screen.getAllByRole('region')).toHaveLength(5)
    await screen.findByRole('list', { name: '사건 목록' })
  })

  it('offers the three demo dates and no empty-story button', () => {
    render(<App />)

    expect(
      screen.getAllByRole('button').map((button) => button.textContent),
    ).toEqual(DEMO_BUTTONS)
    expect(
      screen.queryByRole('button', { name: '빈 결과 화면 보기' }),
    ).not.toBeInTheDocument()
  })

  it('shows the 1988 events in the world section and placeholders elsewhere', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    )

    const list = await screen.findByRole('list', { name: '사건 목록' })
    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
    const others = screen
      .getAllByRole('region')
      .filter((region) => region.id !== 'world')
    expect(others).toHaveLength(4)
    for (const region of others) {
      expect(region).toHaveTextContent('아직 준비 중인 섹션입니다.')
    }
  })

  it('shows the empty-day notice for 1995-03-15', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      screen.getByRole('button', { name: '1995년 3월 15일로 보기' }),
    )

    expect(
      await screen.findByText('이 날짜에 기록된 큰 사건을 찾지 못했습니다.'),
    ).toBeInTheDocument()
  })

  it('cancels the pending request when going back to the start', async () => {
    let signal: AbortSignal | null = null
    server.use(
      http.get('/api/story/world', async ({ request }) => {
        signal = request.signal
        await delay('infinite')
        return HttpResponse.json(world1988)
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    )
    await waitFor(() => expect(signal).not.toBeNull())
    await user.click(screen.getByRole('button', { name: '처음으로' }))

    await waitFor(() => expect(signal!.aborted).toBe(true))
    expect(screen.queryAllByRole('region')).toHaveLength(0)
    expect(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    ).toBeInTheDocument()
  })
})
