// Plan M3b2b T3: real-browser checks that jsdom cannot make. The focus ring
// shows for the keyboard only, design tokens are really painted, nothing
// scrolls sideways at 360px, and nothing reaches the external network.
import { expect, test, type Locator, type Page } from '@playwright/test'
import {
  world1988WithMedia,
  worldWithTragic,
} from '../src/mocks/fixtures/world/responses'
import { interceptNetwork, openDemo, tokenColor } from './network'

const DEMO_1988 = '1988년 9월 17일로 보기'

const outline = (link: Locator) =>
  link.evaluate((element) => {
    const style = getComputedStyle(element)
    return {
      style: style.outlineStyle,
      width: style.outlineWidth,
      color: style.outlineColor,
    }
  })

const css = (locator: Locator, property: 'color' | 'backgroundColor') =>
  locator.evaluate((element, name) => getComputedStyle(element)[name], property)

const isFocused = (locator: Locator) =>
  locator.evaluate((element) => element === document.activeElement)

async function tabTo(page: Page, target: Locator) {
  for (let i = 0; i < 40; i++) {
    await page.keyboard.press('Tab')
    if (await isFocused(target)) return
  }
  throw new Error('Tab never reached the target')
}

const sourceLink = (page: Page) =>
  page
    .getByRole('figure')
    .first()
    .getByRole('link', { name: /^View original/ })

test.describe('1988-09-17 with media', () => {
  let outside: Awaited<ReturnType<typeof interceptNetwork>>

  test.beforeEach(async ({ context, page }) => {
    outside = await interceptNetwork(context, world1988WithMedia)
    await openDemo(page, DEMO_1988)
  })

  test.afterEach(() => {
    expect(outside.answered).toEqual([])
  })

  test('keyboard focus on 원문 보기 shows the --color-focus ring', async ({
    page,
  }) => {
    const link = sourceLink(page)

    await tabTo(page, link)

    const ring = await outline(link)
    expect(ring.style).not.toBe('none')
    expect(parseFloat(ring.width)).toBeGreaterThan(0)
    expect(ring.color).toBe(await tokenColor(page, '--color-focus'))
  })

  test('a mouse click on 원문 보기 shows no ring', async ({
    context,
    page,
  }) => {
    const link = sourceLink(page)
    const popupPromise = context.waitForEvent('page')

    await link.click()
    const popup = await popupPromise
    await popup.close()

    const ring = await outline(link)
    expect(ring.style === 'none' || parseFloat(ring.width) === 0).toBe(true)
    // The new tab's request to the original source was stopped.
    expect(outside.aborted).toContain(await link.getAttribute('href'))
  })

  test('paints the page background and the source link from tokens', async ({
    page,
  }) => {
    // global.css paints the page from :root, so the html element holds it.
    const root = page.locator('html')
    const eventSource = page
      .getByRole('list', { name: 'Events' })
      .getByRole('link', { name: /“9월 17일”/ })
      .first()

    expect(await css(root, 'backgroundColor')).toBe(
      await tokenColor(page, '--color-bg'),
    )
    expect(await css(eventSource, 'color')).toBe(
      await tokenColor(page, '--color-accent'),
    )
  })

  test('has no sideways scroll', async ({ page }) => {
    const { scrollWidth, clientWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }))
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth)
  })
})

test.describe('a day with a tragic event (synthetic)', () => {
  let outside: Awaited<ReturnType<typeof interceptNetwork>>

  test.beforeEach(async ({ context, page }) => {
    outside = await interceptNetwork(context, worldWithTragic)
    await openDemo(page, DEMO_1988)
  })

  test.afterEach(() => {
    expect(outside.answered).toEqual([])
  })

  test('paints the tragic block and its links from --color-tragic-*', async ({
    page,
  }) => {
    const heading = page.getByRole('heading', {
      level: 3,
      name: 'Difficult events on the same day',
    })
    const block = heading.locator('..')
    const links = block.getByRole('link')

    await expect(links).toHaveCount(2)
    expect(await css(block, 'backgroundColor')).toBe(
      await tokenColor(page, '--color-tragic-bg'),
    )
    const tragicText = await tokenColor(page, '--color-tragic-text')
    for (const link of await links.all()) {
      expect(await css(link, 'color')).toBe(tragicText)
    }
    expect(tragicText).not.toBe(await tokenColor(page, '--color-accent'))
  })

  test('has no sideways scroll', async ({ page }) => {
    const { scrollWidth, clientWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }))
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth)
  })
})
