// Plan M2b T4 (4): the input form in a real Chromium. The demo inputs from
// product brief 12 show the opening sentence and the world section without
// changing the address, and the whole flow works with the keyboard alone.
// Nothing reaches the network (e2e/network.ts).
import { expect, test, type Locator, type Page } from '@playwright/test'
import {
  world1988,
  world1995,
  world2002,
} from '../src/mocks/fixtures/world/responses'
import { fillStory, interceptNetwork } from './network'

const CONSENT_LABEL = '개인정보처리방침에 동의합니다 (필수)'

const DEMOS = [
  {
    input: { date: '1988-09-17', place: '서울' },
    world: world1988,
    words: '1988년 9월 17일 토요일, 서울',
  },
  {
    input: { date: '2002-06-22', place: '광주 서구 풍암동', time: '15:20' },
    world: world2002,
    words: '2002년 6월 22일 토요일 오후 3시 20분, 광주 서구 풍암동',
  },
  {
    input: { date: '1995-03-15', place: '서울대학교병원', time: '00:05' },
    world: world1995,
    words: '1995년 3월 15일 수요일 오전 0시 5분, 서울대학교병원',
  },
]

const opening = (page: Page) =>
  page.getByRole('region', { name: '오프닝' }).getByText(/요일/)
const world = (page: Page) => page.getByRole('region', { name: '그날의 세계' })
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
    if (input.time) await page.getByLabel('출생 시각 (선택)').fill(input.time)
    await page.getByRole('checkbox', { name: CONSENT_LABEL }).check()
    await page.getByRole('button', { name: '그날 보기' }).click()

    await expect(opening(page)).toHaveText(words)
    await expect(opening(page)).toBeFocused()
    await expect(world(page)).toBeVisible()
    await expect(world(page)).not.toHaveAttribute('aria-busy', 'true')
    await expect(world(page)).not.toContainText('불러오지 못했습니다')
    expect(page.url()).toBe(start)

    await page.getByRole('button', { name: '처음으로' }).click()
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
  const year = page.getByRole('combobox', { name: '년' })

  // Typing on a focused select picks the option that starts with the text.
  await tabTo(page, year)
  await page.keyboard.type('1988')
  await page.keyboard.press('Tab')
  await page.keyboard.type('9')
  await page.keyboard.press('Tab')
  await page.keyboard.type('17')
  await page.keyboard.press('Tab')
  await page.keyboard.type('서울')
  await tabTo(page, page.getByRole('checkbox', { name: CONSENT_LABEL }))
  await page.keyboard.press('Space')
  await tabTo(page, page.getByRole('button', { name: '그날 보기' }))
  await page.keyboard.press('Enter')

  await expect(opening(page)).toHaveText('1988년 9월 17일 토요일, 서울')
  await expect(opening(page)).toBeFocused()

  await tabTo(page, page.getByRole('button', { name: '처음으로' }))
  await page.keyboard.press('Enter')
  await expect(
    page.getByRole('heading', { level: 1, name: 'The World That Day' }),
  ).toBeFocused()
  await expect(year).toHaveValue('1988')
  await expect(page.getByRole('combobox', { name: '월' })).toHaveValue('9')
  await expect(page.getByRole('combobox', { name: '일' })).toHaveValue('17')
  await expect(
    page.getByRole('textbox', { name: '태어난 장소 (필수)' }),
  ).toHaveValue('서울')
  await expect(
    page.getByRole('checkbox', { name: CONSENT_LABEL }),
  ).toBeChecked()
  expect(page.url()).toBe(start)
})
