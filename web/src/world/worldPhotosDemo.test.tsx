// Plan M3b2b T4 (5): the "데모 날짜로 확인할 것" cells that were only checked
// on a lone card, now checked on the whole screen reached through the demo
// date buttons: caption license lines and links, new-tab notices on every
// new-tab link, and alt text with the descriptions left in. MSW only; demo
// dates from product brief 12 and the synthetic tragic fixtures.
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { agree } from '../test/consent'
import { openStory } from '../test/storyInput'
import {
  captureTimeUnknown,
  licenseUnknown,
  olympicsOpeningPd,
  worldCupKogl1,
} from '../mocks/fixtures/world/media'
import {
  world1988WithMedia,
  worldTragicOnly,
  worldWithTragic,
} from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import type { WorldResponse } from './types'

// The demo dates with their places, entered through the form (M2b T3 (4)).
const DEMO = {
  '1988-09-17': '서울',
  '2002-06-22': '광주 서구 풍암동',
} as const

const NEW_TAB = /\(새 창에서 열림\)/

const world = () => screen.getByRole('region', { name: '그날의 세계' })

async function showDemo(date: keyof typeof DEMO) {
  const user = userEvent.setup()
  render(<App />)
  await agree(user)
  await openStory(user, { date, place: DEMO[date] })
  await waitFor(() => expect(world()).not.toHaveAttribute('aria-busy'))
  expect(screen.queryByText('이 섹션을 불러오지 못했습니다.')).toBeNull()
}

// The block that a subheading opens.
const block = (title: string) =>
  within(world()).getByRole('heading', { level: 3, name: title }).parentElement!

// Caption lines as the eye reads them (hidden notices removed).
const lines = (figure: HTMLElement) =>
  [...figure.querySelectorAll('figcaption > p')].map((p) => {
    const copy = p.cloneNode(true) as HTMLElement
    copy.querySelectorAll('.visually-hidden').forEach((node) => node.remove())
    return (copy.textContent ?? '').replace(/\s+/g, ' ').trim()
  })

// Every link that opens a new tab names it for screen readers.
function expectNewTabNotices() {
  const blank = world().querySelectorAll('a[target="_blank"]')
  expect(blank.length).toBeGreaterThan(0)
  for (const link of blank) {
    expect(link).toHaveAccessibleName(NEW_TAB)
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  }
  expect(within(world()).getAllByRole('link')).toHaveLength(blank.length)
}

describe('demo 1988-09-17 captions on the screen (데모 표 1행)', () => {
  it('event ①’s PD photo: no separate license line and no 라이선스 link', async () => {
    await showDemo('1988-09-17')

    const list = screen.getByRole('list', { name: '사건 목록' })
    const [pd] = within(list).getAllByRole('figure')

    expect(within(pd).getByRole('img')).toHaveAttribute(
      'src',
      olympicsOpeningPd.thumbnail_url,
    )
    expect(lines(pd)).toContain(olympicsOpeningPd.attribution)
    expect(lines(pd)).not.toContain('Public domain')
    expect(within(pd).queryByRole('link', { name: /라이선스/ })).toBeNull()
    expect(
      within(pd).getByRole('link', { name: /^원문 보기.*새 창에서 열림/ }),
    ).toHaveAttribute('href', olympicsOpeningPd.source_url)
  })

  it('the unlicensed day photo keeps its honest "라이선스 확인 안 됨" line', async () => {
    await showDemo('1988-09-17')

    const [figure] = within(block('그날 찍힌 사진')).getAllByRole('figure')

    expect(within(figure).getByRole('img')).toHaveAttribute(
      'src',
      licenseUnknown.thumbnail_url,
    )
    expect(lines(figure)).toContain('라이선스 확인 안 됨')
  })

  it('every new-tab link, caption 원문 보기 included, says "새 창에서 열림"', async () => {
    await showDemo('1988-09-17')

    // Four cards, one 원문 보기 each.
    expect(
      within(world()).getAllByRole('link', {
        name: /^원문 보기.*새 창에서 열림/,
      }),
    ).toHaveLength(4)
    expectNewTabNotices()
  })

  it('photos with a description: alt is the short fallback, the caption has the text', async () => {
    await showDemo('1988-09-17')

    const list = screen.getByRole('list', { name: '사건 목록' })
    const eventAlts = within(list)
      .getAllByRole('img')
      .map((img) => img.getAttribute('alt'))
    const dayImgs = [
      ...within(block('그날 찍힌 사진')).getAllByRole('img'),
      ...within(block('촬영 시점을 확인하지 못한 사진')).getAllByRole('img'),
    ]

    expect(eventAlts).toEqual([
      '「1988년 하계 올림픽」 관련 자료',
      '「1988년 하계 올림픽」 관련 자료',
    ])
    expect(dayImgs.map((img) => img.getAttribute('alt'))).toEqual([
      '기록 사진',
      '기록 사진',
    ])
    // The fixture photos do carry descriptions; none is read twice.
    const described = [olympicsOpeningPd, captureTimeUnknown, licenseUnknown]
      .map((media) => media.description?.trim())
      .filter((text): text is string => Boolean(text))
    expect(described.length).toBeGreaterThan(0)
    for (const img of screen.getAllByRole('img')) {
      const alt = img.getAttribute('alt') ?? ''
      for (const text of described) expect(alt).not.toContain(text)
    }
    const caption = within(list)
      .getAllByRole('figure')[0]
      .querySelector('figcaption')
    expect(caption).toHaveTextContent(olympicsOpeningPd.description)
  })

  it('has no "다른 때 찍힌 사진" heading and no tragic block', async () => {
    await showDemo('1988-09-17')

    expect(
      within(world())
        .getAllByRole('heading', { level: 3 })
        .map((heading) => heading.textContent),
    ).toEqual(['그날 찍힌 사진', '촬영 시점을 확인하지 못한 사진'])
    expect(world1988WithMedia.day_media).toHaveLength(2)
  })
})

describe('demo 2002-06-22 caption on the screen (데모 표 2행)', () => {
  it('KOGL photo: no separate license line, the 라이선스 link stays', async () => {
    await showDemo('2002-06-22')

    const figure = within(world()).getByRole('figure')

    expect(lines(figure)).toContain(worldCupKogl1.attribution)
    expect(lines(figure)).not.toContain('KOGL Type 1')
    expect(
      within(figure).getByRole('link', { name: /^라이선스.*새 창에서 열림/ }),
    ).toHaveAttribute('href', worldCupKogl1.license_url)
    expectNewTabNotices()
  })
})

describe('synthetic tragic days through the demo button', () => {
  const answer = (body: WorldResponse) =>
    server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

  it.each([
    ['worldWithTragic', worldWithTragic],
    ['worldTragicOnly', worldTragicOnly],
  ])(
    '%s: every new-tab link says so, no media in the section',
    async (_, body) => {
      answer(body)
      await showDemo('1988-09-17')

      expect(
        within(block('같은 날 있었던 아픈 일')).getByText(
          '시험용 비극 사건 문장',
        ),
      ).toBeInTheDocument()
      expect(world().querySelector('img, iframe, video')).toBeNull()
      expectNewTabNotices()
    },
  )
})
