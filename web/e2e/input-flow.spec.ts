// Plans M2b T4 (4) and C1 S2 T8a: the input form in a real Chromium. The
// four demo inputs (Canada research decision 1 (b)) show the opening sentence
// and the world section without changing the address, and the whole flow
// works with the keyboard alone. The world answer is a sample fixture, not
// the demo date's. Nothing reaches the network (e2e/network.ts).
import { expect, test, type Locator, type Page } from '@playwright/test'
import {
  world1988,
  world1995,
  world2002,
} from '../src/mocks/fixtures/world/responses'
import { fillStory, interceptNetwork } from './network'

const CONSENT_LABEL = 'I agree to the privacy policy (required)'

const DEMOS = [
  {
    input: { date: '1988-02-13', place: 'Calgary' },
    world: world1988,
    words: 'Saturday, February 13, 1988, Calgary',
  },
  {
    input: {
      date: '1976-07-17',
      place: 'Hochelaga-Maisonneuve, Montréal',
      time: '15:20',
    },
    world: world2002,
    words:
      'Saturday, July 17, 1976 at 3:20 p.m., Hochelaga-Maisonneuve, Montréal',
  },
  {
    input: {
      date: '2010-02-12',
      place: 'Vancouver General Hospital',
      time: '00:05',
    },
    world: world1995,
    words:
      'Friday, February 12, 2010 at 12:05 a.m., Vancouver General Hospital',
  },
  {
    input: { date: '1980-04-12', place: "St. John's" },
    world: world1988,
    words: "Saturday, April 12, 1980, St. John's",
  },
]

const opening = (page: Page) =>
  page
    .getByRole('region', { name: 'Opening' })
    .getByText(/^(Sun|Mon|Tues|Wednes|Thurs|Fri|Satur)day, /)
const world = (page: Page) =>
  page.getByRole('region', { name: 'The world that day' })
const isFocused = (locator: Locator) =>
  locator.evaluate((element) => element === document.activeElement)

// Presses Tab until `target` has focus; Tab may step through the parts of
// the time box first.
async function tabTo(page: Page, target: Locator) {
  for (let i = 0; i < 60; i++) {
    await page.keyboard.press('Tab')
    if (await isFocused(target)) return
  }
  throw new Error('Tab never reached the target')
}

for (const { input, world: answer, words } of DEMOS) {
  test(`${words}: the opening and the world section, same address`, async ({
    context,
    page,
  }) => {
    const outside = await interceptNetwork(context, answer)
    await page.goto('/')
    const start = page.url()
    await fillStory(page, input)
    if (input.time)
      await page.getByLabel('Time of birth (optional)').fill(input.time)
    await page.getByRole('checkbox', { name: CONSENT_LABEL }).check()
    await page.getByRole('button', { name: 'See that day' }).click()

    await expect(opening(page)).toHaveText(words)
    await expect(opening(page)).toBeFocused()
    await expect(world(page)).toBeVisible()
    await expect(world(page)).not.toHaveAttribute('aria-busy', 'true')
    await expect(world(page)).not.toContainText("couldn't be loaded")
    expect(page.url()).toBe(start)

    await page.getByRole('button', { name: 'Start over' }).click()
    expect(page.url()).toBe(start)
    expect(outside.answered).toEqual([])
  })
}

test('keyboard only: fill, agree, submit, and back to the title', async ({
  context,
  page,
}) => {
  await interceptNetwork(context, world1988)
  await page.goto('/')
  const start = page.url()
  const year = page.getByRole('combobox', { name: 'Year' })

  // Typing on a focused select picks the option that starts with the text.
  await tabTo(page, year)
  await page.keyboard.type('1988')
  await page.keyboard.press('Tab')
  await page.keyboard.type('February')
  await page.keyboard.press('Tab')
  await page.keyboard.type('13')
  await page.keyboard.press('Tab')
  await page.keyboard.type('Calgary')
  await tabTo(page, page.getByRole('checkbox', { name: CONSENT_LABEL }))
  await page.keyboard.press('Space')
  await tabTo(page, page.getByRole('button', { name: 'See that day' }))
  await page.keyboard.press('Enter')

  await expect(opening(page)).toHaveText('Saturday, February 13, 1988, Calgary')
  await expect(opening(page)).toBeFocused()

  await tabTo(page, page.getByRole('button', { name: 'Start over' }))
  await page.keyboard.press('Enter')
  await expect(
    page.getByRole('heading', { level: 1, name: 'The World That Day' }),
  ).toBeFocused()
  await expect(year).toHaveValue('1988')
  await expect(page.getByRole('combobox', { name: 'Month' })).toHaveValue('2')
  await expect(page.getByRole('combobox', { name: 'Day' })).toHaveValue('13')
  await expect(
    page.getByRole('textbox', { name: 'Place of birth (required)' }),
  ).toHaveValue('Calgary')
  await expect(
    page.getByRole('checkbox', { name: CONSENT_LABEL }),
  ).toBeChecked()
  expect(page.url()).toBe(start)
})
