// Plan M3b3b T2 (1)~(7), (10): the required privacy consent on the input
// screen. Dates are the demo and sample dates only.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import App from '../App'
import { YOUTUBE_TERMS_URL } from '../external/urls'
import { server } from '../mocks/server'
import screenCss from '../screens/Screen.module.css?raw'
import { agree, consentBox, CONSENT_LABEL } from '../test/consent'
import { CONSENT_HINT, useConsent } from './consent'
import consentCss from './Consent.module.css?raw'
import { ConsentNotice } from './ConsentNotice'
import { ConsentProvider } from './ConsentProvider'

const BUTTONS = [
  ['1988년 9월 17일로 보기', '1988-09-17'],
  ['2002년 6월 22일로 보기', '2002-06-22'],
  ['1995년 3월 15일로 보기', '1995-03-15'],
  ['표본: 1969년 7월 20일로 보기', '1969-07-20'],
  ['표본: 1986년 9월 20일로 보기', '1986-09-20'],
] as const

// Every request the app starts, by path and date.
let asked: string[] = []
const record = ({ request }: { request: Request }) => {
  const url = new URL(request.url)
  asked.push(`${url.pathname}?${url.searchParams.get('date')}`)
}
beforeEach(() => {
  asked = []
  server.events.on('request:start', record)
})
afterEach(() => {
  server.events.removeListener('request:start', record)
})

const buttons = () => screen.getAllByRole('button')

function expectNotAgreed() {
  expect(consentBox()).not.toBeChecked()
  expect(screen.queryByText(CONSENT_HINT)).toBeNull()
  expect(buttons()).toHaveLength(5)
  for (const button of buttons()) {
    expect(button).toHaveAttribute('aria-disabled', 'true')
    expect(button).not.toHaveAttribute('disabled')
  }
}

function expectBlocked() {
  expect(screen.queryAllByRole('region')).toHaveLength(0)
  expect(asked).toEqual([])
  expect(screen.getByText(CONSENT_HINT)).toBeVisible()
  expect(consentBox()).toHaveFocus()
  expect(consentBox()).toHaveAccessibleDescription(CONSENT_HINT)
}

describe('before agreeing (T2 (1)·(2))', () => {
  it('starts unchecked and required, with all five buttons aria-disabled', () => {
    render(<App />)
    expect(consentBox()).toHaveAccessibleName(CONSENT_LABEL)
    expect(consentBox()).toBeRequired()
    expect(consentBox()).not.toHaveAccessibleDescription()
    expectNotAgreed()
  })

  it.each(BUTTONS)('%s does not open the story when clicked', async (label) => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(screen.getByRole('button', { name: label }))
    expectBlocked()
  })

  it.each(BUTTONS)('%s does not open the story with Enter', async (label) => {
    const user = userEvent.setup()
    render(<App />)
    screen.getByRole('button', { name: label }).focus()
    await user.keyboard('{Enter}')
    expectBlocked()
  })
})

describe('after agreeing (T2 (3)·(4))', () => {
  it.each(BUTTONS)('%s opens the story for %s', async (label, date) => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(screen.getByRole('button', { name: label }))
    await agree(user)
    expect(screen.queryByText(CONSENT_HINT)).toBeNull()
    expect(consentBox()).not.toHaveAccessibleDescription()
    for (const button of buttons()) {
      expect(button).not.toHaveAttribute('aria-disabled')
    }
    await user.click(screen.getByRole('button', { name: label }))
    expect(screen.getAllByRole('region')).toHaveLength(5)
    expect(asked).toEqual([`/api/story/world?${date}`])
  })

  it('goes back to the first state when unchecked', async () => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(buttons()[0])
    await agree(user)
    await user.click(consentBox())
    expectNotAgreed()
  })

  it('keeps the agreement after 처음으로, straight to the story', async () => {
    const user = userEvent.setup()
    render(<App />)
    await agree(user)
    await user.click(buttons()[0])
    await screen.findByRole('list', { name: 'Events' })
    await user.click(screen.getByRole('button', { name: 'Start over' }))
    expect(consentBox()).toBeChecked()
    await user.click(buttons()[2])
    expect(screen.getAllByRole('region')).toHaveLength(5)
  })
})

describe('the notice text (T2 (5))', () => {
  it('links the policy and the YouTube terms in new tabs', () => {
    render(<App />)
    const notice = consentBox().closest('div')!
    const policy = within(notice).getByRole('link', {
      name: /^Read the privacy policy\s*\(opens in a new tab\)$/,
    })
    expect(policy).toHaveAttribute('href', '/privacy.html')
    expect(policy).toHaveAttribute('target', '_blank')
    const terms = within(notice).getByRole('link', {
      name: /^YouTube Terms of Service\s*\(opens in a new tab\)$/,
    })
    expect(terms).toHaveAttribute('href', YOUTUBE_TERMS_URL)
    expect(terms).toHaveAttribute('target', '_blank')
    expect(terms).toHaveAttribute('rel', 'noopener noreferrer')

    const text = (notice.textContent ?? '')
      .replaceAll('(opens in a new tab)', '')
      .replace(/\s+/g, ' ')
    expect(text).toContain('This service uses YouTube API Services.')
    expect(text).toContain(
      'By using it, you agree to be bound by the YouTube Terms of Service .',
    )
    // Operator, contact and effective date live on the policy page only.
    expect(text).not.toMatch(/operator|contact|effective date/i)
  })
})

function Probe() {
  return <output>{useConsent() ? 'agreed' : 'not agreed'}</output>
}

describe('useConsent (T2 (6))', () => {
  it('is false outside any provider', () => {
    render(<Probe />)
    expect(screen.getByRole('status')).toHaveTextContent('not agreed')
  })

  it('follows the checkbox inside the provider', async () => {
    const user = userEvent.setup()
    render(
      <ConsentProvider>
        <ConsentNotice />
        <Probe />
      </ConsentProvider>,
    )
    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('not agreed')
    await agree(user)
    expect(status).toHaveTextContent(/^agreed$/)
    await user.click(consentBox())
    expect(status).toHaveTextContent('not agreed')
  })
})

describe('nothing kept in the browser (T2 (7))', () => {
  it('leaves storage, cookies and the address empty after the story', async () => {
    const user = userEvent.setup()
    render(<App />)
    await agree(user)
    await user.click(buttons()[0])
    await screen.findByRole('list', { name: 'Events' })
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(document.cookie).toBe('')
    expect(location.search).toBe('')
    expect(location.hash).toBe('')
  })
})

describe('consent styles (T2 (10))', () => {
  const rule = (css: string, selector: string) =>
    css
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .split('}')
      .find((block) => block.split('{')[0].trim() === selector)
      ?.split('{')[1] ?? ''

  it('rings the focused checkbox with the focus tokens', () => {
    const ring = rule(consentCss, '.box:focus-visible')
    expect(ring).toMatch(
      /outline:\s*var\(--focus-ring-width\)\s+solid\s+var\(--color-focus\)/,
    )
  })

  it('draws aria-disabled buttons with tokens, never opacity', () => {
    const body = rule(screenCss, ".button[aria-disabled='true']")
    expect(body).toMatch(/background:\s*var\(--color-surface\)/)
    expect(body).toMatch(/color:\s*var\(--color-muted\)/)
    expect(body).toMatch(/var\(--color-border\)/)
    expect(screenCss + consentCss).not.toMatch(/opacity\s*:/)
  })
})
