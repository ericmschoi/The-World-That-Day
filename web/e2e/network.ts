import type { BrowserContext, Page } from '@playwright/test'
import type { WorldResponse } from '../src/world/types'

// Tests never call the external network (CLAUDE.md 7.5). Interception sits on
// the context, so new tabs opened by caption links are covered too:
// `/api/story/world` answers with a fixture, any other `/api` path is a 404
// (no 8080 server), and everything off localhost is aborted.
const LOCAL = new Set(['localhost', '127.0.0.1'])

// What happens to requests for the fixture's video files (plan M3b3 T4).
// 'abort' (default): stopped like any other outside request. 'hold': never
// answered, neither data nor error, so the card stays and the play attempt
// can be watched without anything leaving the machine.
export type VideoRequests = 'abort' | 'hold'

const VIDEO_FILE = /^https:\/\/media\.example\.org\/transcoded\//

export async function interceptNetwork(
  context: BrowserContext,
  world: WorldResponse,
  { videos = 'abort' }: { videos?: VideoRequests } = {},
) {
  // Off-localhost requests that got an answer (must stay empty), those that
  // were stopped, and those held unanswered.
  const outside = {
    answered: [] as string[],
    aborted: [] as string[],
    held: [] as string[],
  }
  const isOutside = (url: string) => !LOCAL.has(new URL(url).hostname)
  context.on('requestfinished', (request) => {
    if (isOutside(request.url())) outside.answered.push(request.url())
  })
  context.on('requestfailed', (request) => {
    if (isOutside(request.url())) outside.aborted.push(request.url())
  })
  await context.route('**/*', (route) => {
    const url = new URL(route.request().url())
    // A held route is left unanswered until the context closes.
    if (videos === 'hold' && VIDEO_FILE.test(url.href)) {
      outside.held.push(url.href)
      return
    }
    if (!LOCAL.has(url.hostname)) return route.abort()
    if (url.pathname === '/api/story/world')
      return route.fulfill({ json: world })
    if (url.pathname.startsWith('/api/')) return route.fulfill({ status: 404 })
    return route.continue()
  })
  return outside
}

// From the input screen to the story: agree to the privacy policy
// (M3b3b), then a demo date button.
export async function openDemo(page: Page, label: string) {
  await page.goto('/')
  await page
    .getByRole('checkbox', { name: 'I agree to the privacy policy (required)' })
    .check()
  await page.getByRole('button', { name: label }).click()
  await page
    .getByRole('region', { name: 'The world that day' })
    .and(page.locator(':not([aria-busy])'))
    .waitFor()
}

// The computed value of a colour token, in the same rgb() form the browser
// gives for `color` and `background-color`.
export function tokenColor(page: Page, token: string) {
  return page.evaluate((name) => {
    const probe = document.createElement('div')
    probe.style.color = `var(${name})`
    document.body.append(probe)
    const value = getComputedStyle(probe).color
    probe.remove()
    return value
  }, token)
}
