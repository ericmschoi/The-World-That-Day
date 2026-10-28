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
import {
  fillStory,
  openStory,
  submitButton,
  submitStory,
} from '../test/storyInput'
import { CONSENT_HINT, useConsent } from './consent'
import consentCss from './Consent.module.css?raw'
import { ConsentNotice } from './ConsentNotice'
import { ConsentProvider } from './ConsentProvider'

// Demo pairs (product brief 12) and the sample dates of public events with
// 서울, entered through the form (M2b T3 (4)).
const INPUTS = [
  ['1988-09-17', '서울'],
  ['2002-06-22', '광주 서구 풍암동'],
  ['1995-03-15', '서울대학교병원'],
  ['1969-07-20', '서울'],
  ['1986-09-20', '서울'],
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
  // M2b T3: the form's submit is the only button.
  expect(buttons()).toHaveLength(1)
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
  it('starts unchecked and required, with the submit button aria-disabled', () => {
    render(<App />)
    expect(consentBox()).toHaveAccessibleName(CONSENT_LABEL)
    expect(consentBox()).toBeRequired()
    expect(consentBox()).not.toHaveAccessibleDescription()
    expectNotAgreed()
  })

  it.each(INPUTS)(
    '%s, %s does not open the story when clicked',
    async (date, place) => {
      const user = userEvent.setup()
      render(<App />)
      await openStory(user, { date, place })
      expectBlocked()
    },
  )

  it.each(INPUTS)(
    '%s, %s does not open the story with Enter',
    async (date, place) => {
      const user = userEvent.setup()
      render(<App />)
      await fillStory(user, { date, place })
      submitButton().focus()
      await user.keyboard('{Enter}')
      expectBlocked()
    },
  )
})

describe('after agreeing (T2 (3)·(4))', () => {
  it.each(INPUTS)('%s, %s opens the story', async (date, place) => {
    const user = userEvent.setup()
    render(<App />)
    await openStory(user, { date, place })
    await agree(user)
    expect(screen.queryByText(CONSENT_HINT)).toBeNull()
    expect(consentBox()).not.toHaveAccessibleDescription()
    for (const button of buttons()) {
      expect(button).not.toHaveAttribute('aria-disabled')
    }
    await submitStory(user)
    expect(screen.getAllByRole('region')).toHaveLength(5)
    expect(asked).toEqual([`/api/story/world?${date}`])
  })

  it('goes back to the first state when unchecked', async () => {
    const user = userEvent.setup()
    render(<App />)
    await openStory(user, { date: '1988-09-17', place: '서울' })
    await agree(user)
    await user.click(consentBox())
    expectNotAgreed()
  })

  it('keeps the agreement after 처음으로, straight to the story', async () => {
    const user = userEvent.setup()
    render(<App />)
    await agree(user)
    await openStory(user, { date: '1988-09-17', place: '서울' })
    await screen.findByRole('list', { name: '사건 목록' })
    await user.click(screen.getByRole('button', { name: '처음으로' }))
    expect(consentBox()).toBeChecked()
    await openStory(user, { date: '1995-03-15', place: '서울대학교병원' })
    expect(screen.getAllByRole('region')).toHaveLength(5)
  })
})

describe('the notice text (T2 (5))', () => {
  it('links the policy and the YouTube terms in new tabs', () => {
    render(<App />)
    const notice = consentBox().closest('div')!
    const policy = within(notice).getByRole('link', {
      name: /^개인정보처리방침 보기\s*\(새 창에서 열림\)$/,
    })
    expect(policy).toHaveAttribute('href', '/privacy.html')
    expect(policy).toHaveAttribute('target', '_blank')
    const terms = within(notice).getByRole('link', {
      name: /^YouTube 이용약관\s*\(새 창에서 열림\)$/,
    })
    expect(terms).toHaveAttribute('href', YOUTUBE_TERMS_URL)
    expect(terms).toHaveAttribute('target', '_blank')
    expect(terms).toHaveAttribute('rel', 'noopener noreferrer')

    const text = (notice.textContent ?? '')
      .replaceAll('(새 창에서 열림)', '')
      .replace(/\s+/g, ' ')
    expect(text).toContain('이 서비스는 YouTube API 서비스를 사용합니다.')
    expect(text).toContain(
      '이 서비스를 이용하면 YouTube 이용약관 에 동의하게 됩니다.',
    )
    // Operator, contact and effective date live on the policy page only.
    expect(text).not.toMatch(/운영자|문의|시행일/)
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
    await openStory(user, { date: '1988-09-17', place: '서울' })
    await screen.findByRole('list', { name: '사건 목록' })
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
