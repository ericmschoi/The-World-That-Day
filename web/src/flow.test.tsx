// Plan M2b T4 (1)~(3): the opening sentence, focus moves between the screens
// and the address and storage rules C1~C5. Demo pairs (product brief 12) only.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import indexHtml from '../index.html?raw'
import App from './App'
import { server } from './mocks/server'
import { agree, consentBox } from './test/consent'
import {
  openStory,
  placeBox,
  submitButton,
  TIME_LABEL,
  yearBox,
  type StoryInputValues,
} from './test/storyInput'

const DEMOS: [StoryInputValues, string][] = [
  [{ date: '1988-09-17', place: '서울' }, '1988년 9월 17일 토요일, 서울'],
  [
    { date: '2002-06-22', place: '광주 서구 풍암동', time: '15:20' },
    '2002년 6월 22일 토요일 오후 3시 20분, 광주 서구 풍암동',
  ],
  [
    { date: '1995-03-15', place: '서울대학교병원', time: '00:05' },
    '1995년 3월 15일 수요일 오전 0시 5분, 서울대학교병원',
  ],
]

const opening = () =>
  within(screen.getByRole('region', { name: '오프닝' })).getByText(/요일/)

// Every request URL the app starts.
let asked: string[] = []
const record = ({ request }: { request: Request }) => {
  asked.push(request.url)
}
beforeEach(() => {
  asked = []
  server.events.on('request:start', record)
})
afterEach(() => {
  server.events.removeListener('request:start', record)
  vi.restoreAllMocks()
})

async function start() {
  const user = userEvent.setup()
  render(<App />)
  await agree(user)
  return user
}

describe('opening (T4 (1))', () => {
  it.each(DEMOS)('%o reads as the plan table says', async (input, words) => {
    const user = await start()
    await openStory(user, input)
    expect(opening()).toHaveTextContent(new RegExp(`^${words}$`))
    expect(screen.getAllByRole('region')[0]).toHaveAccessibleName('오프닝')
  })

  it.each([
    ['12:00', '1988년 9월 17일 토요일 오후 12시, 서울'],
    ['09:00', '1988년 9월 17일 토요일 오전 9시, 서울'],
  ])('time %s leaves out zero minutes', async (time, words) => {
    const user = await start()
    await openStory(user, { date: '1988-09-17', place: '서울', time })
    expect(opening()).toHaveTextContent(new RegExp(`^${words}$`))
  })
})

describe('focus and kept values (T4 (2))', () => {
  it('moves to the opening, then back to the title with the input kept', async () => {
    const user = await start()
    await openStory(user, DEMOS[1][0])
    expect(document.activeElement).toBe(opening())
    expect(opening()).toHaveAttribute('tabindex', '-1')

    await user.click(screen.getByRole('button', { name: '처음으로' }))
    expect(
      screen.getByRole('heading', { level: 1, name: 'The World That Day' }),
    ).toHaveFocus()
    expect(yearBox()).toHaveValue('2002')
    expect(screen.getByRole('combobox', { name: '월' })).toHaveValue('6')
    expect(screen.getByRole('combobox', { name: '일' })).toHaveValue('22')
    expect(placeBox()).toHaveValue('광주 서구 풍암동')
    expect(screen.getByLabelText(TIME_LABEL)).toHaveValue('15:20')
    expect(consentBox()).toBeChecked()

    // The kept input goes straight to the same story again.
    await user.click(submitButton())
    expect(opening()).toHaveTextContent(DEMOS[1][1])
  })

  it('does not move focus on the first visit', () => {
    render(<App />)
    expect(document.body).toHaveFocus()
  })
})

describe('address and storage rules (T4 (3))', () => {
  it('C1~C3: same address, no history calls, nothing stored, date only sent', async () => {
    const push = vi.spyOn(history, 'pushState')
    const replace = vi.spyOn(history, 'replaceState')
    const startHref = location.href
    const user = await start()

    for (const [input] of DEMOS) {
      await openStory(user, input)
      await screen.findByRole('region', { name: '그날의 세계' })
      expect(location.href).toBe(startHref)
      await user.click(screen.getByRole('button', { name: '처음으로' }))
      expect(location.href).toBe(startHref)
    }

    expect(push).not.toHaveBeenCalled()
    expect(replace).not.toHaveBeenCalled()
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(document.cookie).toBe('')
    expect(
      asked.map((url) => new URL(url).pathname + new URL(url).search),
    ).toEqual(DEMOS.map(([{ date }]) => `/api/story/world?date=${date}`))
    for (const url of asked) {
      const plain = decodeURIComponent(url)
      for (const [{ place, time }] of DEMOS) {
        expect(plain).not.toContain(place)
        if (time) expect(plain).not.toContain(time)
      }
    }
  })

  it('C4: index.html sets strict-origin-when-cross-origin, never no-referrer', () => {
    const doc = new DOMParser().parseFromString(indexHtml, 'text/html')
    const metas = doc.querySelectorAll('meta[name="referrer"]')
    expect(metas).toHaveLength(1)
    expect(metas[0].getAttribute('content')).toBe(
      'strict-origin-when-cross-origin',
    )
    expect(indexHtml).not.toMatch(/content="no-referrer"/)
  })

  it('C5: the form and its typed fields turn autocomplete off', () => {
    render(<App />)
    expect(submitButton().closest('form')).toHaveAttribute(
      'autocomplete',
      'off',
    )
    expect(placeBox()).toHaveAttribute('autocomplete', 'off')
    expect(screen.getByLabelText(TIME_LABEL)).toHaveAttribute(
      'autocomplete',
      'off',
    )
  })
})
