// Plan M3b2b T4, about T3 (4) "body 배경이 --color-bg": global.css paints
// the page from :root, and CSS carries the root background to the whole
// canvas. This pins what the reader sees: html holds --color-bg and body
// paints nothing over it, so the page behind body is the token colour too.
import { expect, test } from '@playwright/test'
import { world1988WithMedia } from '../src/mocks/fixtures/world/responses'
import { interceptNetwork, openStory, tokenColor } from './network'

const TRANSPARENT = 'rgba(0, 0, 0, 0)'

test('the page shows --color-bg: html holds it, body paints nothing over it', async ({
  context,
  page,
}) => {
  const outside = await interceptNetwork(context, world1988WithMedia)
  await openStory(page, { date: '1988-09-17', place: '서울' })

  const background = (selector: string) =>
    page
      .locator(selector)
      .evaluate((element) => getComputedStyle(element).backgroundColor)
  const bg = await tokenColor(page, '--color-bg')

  expect(bg).not.toBe(TRANSPARENT)
  expect(await background('html')).toBe(bg)
  expect([TRANSPARENT, bg]).toContain(await background('body'))
  expect(outside.answered).toEqual([])
})
