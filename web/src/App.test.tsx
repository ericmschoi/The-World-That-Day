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

// A sample public-event date and the boundary date with Calgary, entered
// through the form (M2b T3 (4), C1 S2-P1 (a)).
const DEMO_1988 = { date: '1988-09-17', place: 'Calgary' }
const EMPTY_1950 = { date: '1950-01-01', place: 'Calgary' }

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
    await screen.findByRole('list', { name: 'Events' })

    await user.click(screen.getByRole('button', { name: 'Start over' }))
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
      screen.getByRole('link', { name: /^Read the privacy policy/ }),
    ).toHaveFocus()
    await user.tab()
    expect(
      screen.getByRole('link', { name: /^YouTube Terms of Service/ }),
    ).toHaveFocus()
    await user.tab()
    expect(submitButton()).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(screen.getAllByRole('region')).toHaveLength(5)
    await screen.findByRole('list', { name: 'Events' })
  })

  it('offers only the submit button and no empty-story button', () => {
    render(<App />)

    expect(
      screen.getAllByRole('button').map((button) => button.textContent),
    ).toEqual(['See that day'])
    expect(
      screen.queryByRole('button', { name: '빈 결과 화면 보기' }),
    ).not.toBeInTheDocument()
  })

  it('shows the 1988 events in the world section and placeholders elsewhere', async () => {
    const user = userEvent.setup()
    render(<App />)

    await agree(user)
    await openStory(user, DEMO_1988)

    const list = await screen.findByRole('list', { name: 'Events' })
    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
    const others = screen
      .getAllByRole('region')
      .filter((region) => region.id !== 'world')
    expect(others).toHaveLength(4)
    // M2b T4: the opening is filled now; the other three stay placeholders.
    expect(others[0]).toHaveTextContent('Saturday, September 17, 1988, Calgary')
    for (const region of others.slice(1)) {
      expect(region).toHaveTextContent('This section is coming soon.')
    }
  })

  it('shows the empty-day notice for 1950-01-01', async () => {
    const user = userEvent.setup()
    render(<App />)

    await agree(user)
    await openStory(user, EMPTY_1950)

    expect(
      await screen.findByText('No major events were found for this date.'),
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
    await user.click(screen.getByRole('button', { name: 'Start over' }))

    await waitFor(() => expect(signal!.aborted).toBe(true))
    expect(screen.queryAllByRole('region')).toHaveLength(0)
    expect(submitButton()).toBeInTheDocument()
  })
})
