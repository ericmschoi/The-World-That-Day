import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from './App'
import { world1988 } from './mocks/fixtures/world/responses'
import { server } from './mocks/server'
import { agree, consentBox } from './test/consent'
import {
  fillStory,
  openStory,
  submitButton,
  TIME_LABEL,
} from './test/storyInput'

// Demo pairs from product brief 12, entered through the form (M2b T3 (4)).
const DEMO_1988 = { date: '1988-09-17', place: '서울' }
const DEMO_1995 = { date: '1995-03-15', place: '서울대학교병원' }

describe('App', () => {
  it('switches between the input placeholder and the story', async () => {
    const user = userEvent.setup()
    render(<App />)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { level: 1, name: 'The World That Day' }),
    ).toBeInTheDocument()
    expect(screen.queryAllByRole('region')).toHaveLength(0)

    await agree(user)
    await openStory(user, DEMO_1988)
    expect(screen.getAllByRole('region')).toHaveLength(5)
    await screen.findByRole('list', { name: '사건 목록' })

    await user.click(screen.getByRole('button', { name: '처음으로' }))
    expect(screen.queryAllByRole('region')).toHaveLength(0)
    expect(submitButton()).toBeInTheDocument()
  })

  it('works with the keyboard alone', async () => {
    const user = userEvent.setup()
    render(<App />)

    // M2b T3: the fields are filled first (jsdom cannot pick a select option
    // by keyboard; Playwright does in T4), then from the time field on:
    // M3b3b T2 (8): checkbox, policy link, terms link, then the button.
    await fillStory(user, DEMO_1988)
    screen.getByLabelText(TIME_LABEL).focus()
    await user.tab()
    expect(consentBox()).toHaveFocus()
    await user.keyboard(' ')
    expect(consentBox()).toBeChecked()
    await user.tab()
    expect(
      screen.getByRole('link', { name: /^개인정보처리방침 보기/ }),
    ).toHaveFocus()
    await user.tab()
    expect(
      screen.getByRole('link', { name: /^YouTube 이용약관/ }),
    ).toHaveFocus()
    await user.tab()
    expect(submitButton()).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(screen.getAllByRole('region')).toHaveLength(5)
    await screen.findByRole('list', { name: '사건 목록' })
  })

  // M2b T3 (3): the demo date buttons are gone; the form's submit is the
  // only button on the input screen.
  it('offers the three demo dates and no empty-story button', () => {
    render(<App />)

    expect(
      screen.getAllByRole('button').map((button) => button.textContent),
    ).toEqual(['그날 보기'])
    expect(
      screen.queryByRole('button', { name: '빈 결과 화면 보기' }),
    ).not.toBeInTheDocument()
  })

  it('shows the 1988 events in the world section and placeholders elsewhere', async () => {
    const user = userEvent.setup()
    render(<App />)

    await agree(user)
    await openStory(user, DEMO_1988)

    const list = await screen.findByRole('list', { name: '사건 목록' })
    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
    const others = screen
      .getAllByRole('region')
      .filter((region) => region.id !== 'world')
    expect(others).toHaveLength(4)
    // M2b T4: the opening is filled now; the other three stay placeholders.
    expect(others[0]).toHaveTextContent('1988년 9월 17일 토요일, 서울')
    for (const region of others.slice(1)) {
      expect(region).toHaveTextContent('아직 준비 중인 섹션입니다.')
    }
  })

  it('shows the empty-day notice for 1995-03-15', async () => {
    const user = userEvent.setup()
    render(<App />)

    await agree(user)
    await openStory(user, DEMO_1995)

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

    await agree(user)
    await openStory(user, DEMO_1988)
    await waitFor(() => expect(signal).not.toBeNull())
    await user.click(screen.getByRole('button', { name: '처음으로' }))

    await waitFor(() => expect(signal!.aborted).toBe(true))
    expect(screen.queryAllByRole('region')).toHaveLength(0)
    expect(submitButton()).toBeInTheDocument()
  })
})
