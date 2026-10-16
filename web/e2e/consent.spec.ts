// Plan M3b3b T3 (2): the required privacy consent and the policy page in a
// real Chromium. Nothing reaches the network: the story is the synthetic
// `world1988` fixture, every outside request is aborted, and the GitHub
// Issues and YouTube links are checked by `href` only, never opened.
import { expect, test, type Page } from '@playwright/test'
import { CONTACT_ISSUES_URL } from '../src/external/urls'
import { world1988 } from '../src/mocks/fixtures/world/responses'
import { interceptNetwork, openDemo, tokenColor } from './network'

const DEMO_1988 = '1988년 9월 17일로 보기'
const CONSENT_LABEL = '개인정보처리방침에 동의합니다 (필수)'
const CONSENT_HINT = '개인정보처리방침에 동의해야 볼 수 있습니다.'
const BUTTONS = [
  DEMO_1988,
  '2002년 6월 22일로 보기',
  '1995년 3월 15일로 보기',
  '표본: 1969년 7월 20일로 보기',
  '표본: 1986년 9월 20일로 보기',
]
const LOCAL = new Set(['localhost', '127.0.0.1'])

const consentBox = (page: Page) =>
  page.getByRole('checkbox', { name: CONSENT_LABEL })
const world = (page: Page) => page.getByRole('region', { name: '그날의 세계' })
const footerLink = (page: Page) =>
  page.getByRole('contentinfo').getByRole('link', { name: /^개인정보처리방침/ })

// The dates asked for, in order. React's StrictMode in the dev server may
// ask twice for the same date, so repeats are folded.
const askedDates = () => [
  ...new Set(storyRequests.map((url) => new URL(url).searchParams.get('date'))),
]

let outside: Awaited<ReturnType<typeof interceptNetwork>>
let storyRequests: string[]
let pageErrors: string[]

test.beforeEach(async ({ context, page }) => {
  pageErrors = []
  storyRequests = []
  // (f) Uncaught errors from this page and from any tab it opens.
  const watch = (target: Page) =>
    target.on('pageerror', (error) => pageErrors.push(error.message))
  watch(page)
  context.on('page', watch)
  context.on('request', (request) => {
    if (new URL(request.url()).pathname === '/api/story/world') {
      storyRequests.push(request.url())
    }
  })
  outside = await interceptNetwork(context, world1988)
})

// (f) No uncaught error anywhere, and no outside request even attempted:
// the input screen, the story without media and the policy page load
// nothing from outside.
test.afterEach(() => {
  expect(pageErrors).toEqual([])
  expect(outside.answered).toEqual([])
  expect(outside.aborted).toEqual([])
})

test.describe('before agreeing', () => {
  test('(a) a click explains, focuses the checkbox and asks for nothing', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(consentBox(page)).not.toBeChecked()
    for (const label of BUTTONS) {
      await expect(page.getByRole('button', { name: label })).toHaveAttribute(
        'aria-disabled',
        'true',
      )
    }

    // Playwright waits for aria-disabled elements to become enabled; a
    // reader can still press the button, so the click is forced (a real
    // mouse click at the button, only the wait is skipped).
    await page.getByRole('button', { name: DEMO_1988 }).click({ force: true })
    await expect(page.getByText(CONSENT_HINT)).toBeVisible()
    await expect(consentBox(page)).toBeFocused()
    await expect(consentBox(page)).toHaveAccessibleDescription(CONSENT_HINT)
    // Give a stray request time to show up before saying there is none.
    await page.waitForTimeout(300)
    await expect(world(page)).toHaveCount(0)
    expect(storyRequests).toEqual([])
  })

  test('(a) Enter on a focused button does the same', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: BUTTONS[4] }).focus()
    await page.keyboard.press('Enter')
    await expect(page.getByText(CONSENT_HINT)).toBeVisible()
    await expect(consentBox(page)).toBeFocused()
    await page.waitForTimeout(300)
    await expect(world(page)).toHaveCount(0)
    expect(storyRequests).toEqual([])
  })

  test('Tab goes checkbox, policy, terms, the five buttons, footer', async ({
    page,
  }) => {
    await page.goto('/')
    const order: string[] = []
    for (let i = 0; i < 9; i++) {
      await page.keyboard.press('Tab')
      order.push(
        await page.evaluate(() => {
          const el = document.activeElement as HTMLElement
          const name =
            el instanceof HTMLInputElement
              ? (el.labels?.[0]?.textContent ?? '')
              : (el.textContent ?? '')
          return `${el.tagName.toLowerCase()}:${name.replace(/\s+/g, ' ').trim()}`
        }),
      )
    }
    expect(order).toEqual([
      `input:${CONSENT_LABEL}`,
      'a:개인정보처리방침 보기 (새 창에서 열림)',
      'a:YouTube 이용약관 (새 창에서 열림)',
      ...BUTTONS.map((label) => `button:${label}`),
      'a:개인정보처리방침 (새 창에서 열림)',
    ])
  })
})

test('(b) after agreeing the button opens the story', async ({ page }) => {
  await page.goto('/')
  await consentBox(page).check()
  await expect(page.getByText(CONSENT_HINT)).toHaveCount(0)
  await expect(
    page.getByRole('button', { name: DEMO_1988 }),
  ).not.toHaveAttribute('aria-disabled', 'true')
  await page.getByRole('button', { name: DEMO_1988 }).click()
  await expect(
    world(page).getByRole('list', { name: '사건 목록' }).getByRole('listitem'),
  ).toHaveCount(3)
  expect(askedDates()).toEqual(['1988-09-17'])
})

// (c) The policy page opened from the footer, in a new tab of the same
// (intercepted) context.
async function openPolicyFrom(page: Page) {
  const link = footerLink(page)
  await expect(link).toHaveAttribute('href', '/privacy.html')
  await expect(link).toHaveAttribute('target', '_blank')
  const [policy] = await Promise.all([
    page.context().waitForEvent('page'),
    link.click(),
  ])
  await policy.waitForLoadState('load')
  return policy
}

async function expectPolicy(policy: Page, opener: Page) {
  expect(new URL(policy.url()).pathname).toBe('/privacy.html')
  await expect(policy.getByRole('heading', { level: 1 })).toHaveText(
    '개인정보처리방침',
  )
  await expect(policy.getByRole('main')).toContainText(
    'The World That Day 운영자',
  )
  // Checked by href only, never clicked.
  await expect(
    policy.getByRole('link', { name: 'GitHub Issues', exact: true }),
  ).toHaveAttribute('href', CONTACT_ISSUES_URL)

  // Only our dev server's scripts (Vite adds /@vite/client); the page itself
  // has none (checked on the source file and the build, B1).
  const scripts = await policy
    .locator('script[src]')
    .evaluateAll((elements) =>
      elements.map((element) => (element as HTMLScriptElement).src),
    )
  for (const src of scripts) expect(LOCAL.has(new URL(src).hostname)).toBe(true)

  // privacy.css really applies: the token background and column width.
  const bg = await tokenColor(policy, '--color-bg')
  expect(
    await policy
      .locator('html')
      .evaluate((element) => getComputedStyle(element).backgroundColor),
  ).toBe(bg)
  const widths = await policy.locator('main').evaluate((element) => {
    const probe = document.createElement('div')
    probe.style.maxWidth = 'var(--content-max-width)'
    document.body.append(probe)
    const token = getComputedStyle(probe).maxWidth
    probe.remove()
    return { main: getComputedStyle(element).maxWidth, token }
  })
  expect(widths.main).not.toBe('none')
  expect(widths.main).toBe(widths.token)

  // Same viewport as the app tab (360 or 1280), and nothing scrolls sideways.
  expect(policy.viewportSize()).toEqual(opener.viewportSize())
  const { scrollWidth, clientWidth } = await policy.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }))
  expect(scrollWidth).toBeLessThanOrEqual(clientWidth)
}

test.describe('(c) the footer link opens the policy in a new tab', () => {
  test('from the input screen', async ({ page }) => {
    await page.goto('/')
    const policy = await openPolicyFrom(page)
    await expectPolicy(policy, page)
    // The app tab is left as it was.
    expect(new URL(page.url()).pathname).toBe('/')
    await expect(consentBox(page)).toBeVisible()
  })

  test('from the result screen, which stays open', async ({ page }) => {
    await openDemo(page, DEMO_1988)
    const policy = await openPolicyFrom(page)
    await expectPolicy(policy, page)
    await expect(
      world(page)
        .getByRole('list', { name: '사건 목록' })
        .getByRole('listitem'),
    ).toHaveCount(3)
  })
})

test('(d) the agreement survives 처음으로 and not a reload', async ({
  page,
}) => {
  await openDemo(page, DEMO_1988)
  await page.getByRole('button', { name: '처음으로' }).click()
  await expect(consentBox(page)).toBeChecked()
  await page.getByRole('button', { name: BUTTONS[1] }).click()
  await expect(world(page)).toBeVisible()
  await expect.poll(askedDates).toEqual(['1988-09-17', '2002-06-22'])

  await page.reload()
  await expect(consentBox(page)).not.toBeChecked()
  for (const label of BUTTONS) {
    await expect(page.getByRole('button', { name: label })).toHaveAttribute(
      'aria-disabled',
      'true',
    )
  }
})

test('(e) nothing is kept in the browser after agreeing and the story', async ({
  context,
  page,
}) => {
  await openDemo(page, DEMO_1988)
  const kept = await page.evaluate(() => ({
    local: localStorage.length,
    session: sessionStorage.length,
    cookie: document.cookie,
    search: location.search,
    hash: location.hash,
  }))
  expect(kept).toEqual({
    local: 0,
    session: 0,
    cookie: '',
    search: '',
    hash: '',
  })
  expect(await context.cookies()).toEqual([])
})
