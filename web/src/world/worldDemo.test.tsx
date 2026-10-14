// Plan M3b2 T4: the "데모 입력 3개로 확인할 것" table (automatic column),
// driven from the demo date buttons through the whole App, plus the product
// rules this feature touches: section-only failure, no date in the address,
// browser storage or console, inline media only, safe source links.
// MSW only; demo dates from product brief 12 and nothing else.
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { olympicsOpeningPd } from '../mocks/fixtures/world/media'
import {
  world1988,
  world1988WithMedia,
  world2002,
  world2002WithMedia,
} from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import type { WorldEvent, WorldResponse } from './types'

afterEach(() => {
  vi.restoreAllMocks()
})

const NOTICE = '이 날짜에 기록된 큰 사건을 찾지 못했습니다.'
const ERROR = '이 섹션을 불러오지 못했습니다.'
const PLACEHOLDER = '아직 준비 중인 섹션입니다.'
const DEMO = {
  '1988-09-17': '1988년 9월 17일로 보기',
  '2002-06-22': '2002년 6월 22일로 보기',
  '1995-03-15': '1995년 3월 15일로 보기',
} as const
type DemoDate = keyof typeof DEMO
const DATE_STRINGS = [
  '1988-09-17',
  '2002-06-22',
  '1995-03-15',
  '1988년 9월 17일',
  '2002년 6월 22일',
  '1995년 3월 15일',
]

const world = () => screen.getByRole('region', { name: '그날의 세계' })
const eventList = () => screen.findByRole('list', { name: '사건 목록' })
const answer = (body: WorldResponse) =>
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

async function openDemo(date: DemoDate) {
  const user = userEvent.setup()
  render(<App />)
  await user.click(screen.getByRole('button', { name: DEMO[date] }))
  return user
}

// "출처: {source_name} 「{source_title}」 · {text_license}" with the hidden
// "(새 창에서 열림)" texts removed, whitespace collapsed. The media cards'
// own caption links live inside their figure and are skipped.
const sourceLinks = (item: HTMLElement) =>
  within(item)
    .getAllByRole('link')
    .filter((link) => !link.closest('figure'))

function visibleSourceLine(item: HTMLElement): string {
  const line = sourceLinks(item)[0].closest('p')!.cloneNode(true) as HTMLElement
  line.querySelectorAll('.visually-hidden').forEach((node) => node.remove())
  return (line.textContent ?? '').replace(/\s+/g, ' ').trim()
}

function expectSourceLines(items: HTMLElement[], events: WorldEvent[]) {
  expect(items).toHaveLength(events.length)
  events.forEach((event, i) => {
    expect(visibleSourceLine(items[i])).toBe(
      `출처: 한국어 위키백과 「${event.source_title}」 · CC BY-SA 4.0`,
    )
    const links = sourceLinks(items[i])
    expect(links).toHaveLength(2)
    const [source, license] = links
    expect(source).toHaveAttribute('href', event.source_url)
    expect(license).toHaveAttribute('href', event.text_license_url)
  })
}

describe('demo 1988-09-17 (데모 표 1행)', () => {
  it('shows the three events in order, text exactly as written, with source lines', async () => {
    await openDemo('1988-09-17')

    const items = within(await eventList()).getAllByRole('listitem')

    expect(items.map((item) => item.querySelector('p')?.textContent)).toEqual([
      '제24회 서울 하계 올림픽 개막.',
      'KBS 1TV의 채널에서 광고 24개를 때려갔다.',
      'KBS 2TV의 채널에서 광고 37개를 때려갔다.',
    ])
    expect(screen.getAllByText(/서울 하계 올림픽/)).toHaveLength(1)
    expectSourceLines(items, world1988WithMedia.events)
  })

  it('media fixture: event ① has two inline photos with honest date lines, day_media is outside the list', async () => {
    await openDemo('1988-09-17')

    const list = await eventList()
    const items = within(list).getAllByRole('listitem')
    const [pd, undated] = within(items[0]).getAllByRole('figure')

    // Event ①'s two photos in the list; day_media's two come after it.
    expect(within(list).getAllByRole('img')).toHaveLength(2)
    expect(within(list).getAllByRole('figure')).toHaveLength(2)
    expect(screen.getAllByRole('img')).toHaveLength(4)
    const pdSrc = within(pd).getByRole('img').getAttribute('src')!
    expect(new URL(pdSrc).host).toBe('thumb.wikimedia.org')
    expect(pdSrc).toBe(olympicsOpeningPd.thumbnail_url)
    expect(within(pd).getByText('같은 날')).toBeInTheDocument()
    expect(within(undated).getByText('촬영 시점 미상')).toBeInTheDocument()
    expect(within(undated).queryByText('같은 날')).not.toBeInTheDocument()
    // The unlicensed day_media photo says "라이선스 확인 안 됨", outside.
    expect(
      within(list).queryByText('라이선스 확인 안 됨'),
    ).not.toBeInTheDocument()
    expect(document.querySelector('iframe, video')).toBeNull()
  })

  it('today’s server shape: the same three events and no img', async () => {
    answer(world1988)
    await openDemo('1988-09-17')

    const items = within(await eventList()).getAllByRole('listitem')

    expect(items).toHaveLength(3)
    expectSourceLines(items, world1988.events)
    expect(screen.queryAllByRole('img')).toHaveLength(0)
    expect(screen.queryAllByRole('figure')).toHaveLength(0)
  })
})

describe('demo 2002-06-22 (데모 표 2행)', () => {
  const [quarterFinal] = world2002.events

  it('media fixture: the "스페인" sentence once, source line 「6월 22일」, one KOGL Type 1 photo', async () => {
    await openDemo('2002-06-22')

    const items = within(await eventList()).getAllByRole('listitem')

    expect(items).toHaveLength(1)
    expect(quarterFinal.summary).toContain('스페인')
    expect(
      screen.getAllByText(quarterFinal.summary, { exact: true }),
    ).toHaveLength(1)
    expect(visibleSourceLine(items[0])).toBe(
      '출처: 한국어 위키백과 「6월 22일」 · CC BY-SA 4.0',
    )
    expectSourceLines(items, world2002WithMedia.events)
    const figure = within(items[0]).getByRole('figure')
    expect(screen.getAllByRole('img')).toHaveLength(1)
    // The attribution names KOGL Type 1, so there is no separate line.
    expect(figure).toHaveTextContent('KOGL Type 1')
    expect(within(figure).queryByText('KOGL Type 1')).not.toBeInTheDocument()
    expect(within(figure).getByText('같은 날')).toBeInTheDocument()
    expect(
      new URL(within(figure).getByRole('img').getAttribute('src')!).host,
    ).toBe('thumb.wikimedia.org')
  })

  it('today’s server shape: sentence and source line, no img', async () => {
    answer(world2002)
    await openDemo('2002-06-22')

    const items = within(await eventList()).getAllByRole('listitem')

    expect(items).toHaveLength(1)
    expect(within(items[0]).getByText(quarterFinal.summary)).toBeInTheDocument()
    expectSourceLines(items, world2002.events)
    expect(screen.queryAllByRole('img')).toHaveLength(0)
  })
})

describe('demo 1995-03-15 (데모 표 3행)', () => {
  it('shows the notice under "그날의 세계", no img and no list', async () => {
    await openDemo('1995-03-15')

    expect(await screen.findByText(NOTICE)).toBeInTheDocument()
    expect(
      within(world()).getByRole('heading', { level: 2, name: '그날의 세계' }),
    ).toBeInTheDocument()
    expect(within(world()).getByText(NOTICE)).toBeInTheDocument()
    expect(screen.queryAllByRole('img')).toHaveLength(0)
    expect(screen.queryByRole('list', { name: '사건 목록' })).toBeNull()
  })
})

describe('demo buttons send their own date only', () => {
  it.each(Object.entries(DEMO) as [DemoDate, string][])('%s', async (date) => {
    const sent: string[] = []
    server.events.on('request:start', ({ request }) => {
      sent.push(request.url)
    })
    try {
      await openDemo(date)
      await waitFor(() => expect(world()).not.toHaveAttribute('aria-busy'))
    } finally {
      server.events.removeAllListeners('request:start')
    }

    expect(sent).toHaveLength(1)
    const url = new URL(sent[0])
    expect(url.pathname).toBe('/api/story/world')
    expect([...url.searchParams]).toEqual([['date', date]])
  })
})

describe('failure isolation: only the world section fails (product brief 8)', () => {
  it.each([
    [
      '503 UPSTREAM_UNAVAILABLE',
      () =>
        HttpResponse.json(
          { status: 503, code: 'UPSTREAM_UNAVAILABLE', detail: 'x' },
          {
            status: 503,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
    ],
    ['network error', () => HttpResponse.error()],
    ['500 without a body', () => new HttpResponse(null, { status: 500 })],
  ])('%s', async (_name, respond) => {
    server.use(http.get('/api/story/world', respond))
    const user = await openDemo('1988-09-17')

    expect(await within(world()).findByText(ERROR)).toBeInTheDocument()
    const others = screen
      .getAllByRole('region')
      .filter((region) => region !== world())
    expect(others.map((region) => region.id)).toEqual([
      'opening',
      'neighborhood',
      'city',
      'closing',
    ])
    for (const region of others) {
      expect(region).toHaveTextContent(PLACEHOLDER)
      expect(region).not.toHaveTextContent(ERROR)
    }
    expect(screen.getAllByText(ERROR)).toHaveLength(1)
    expect(
      screen.getByRole('heading', { level: 1, name: 'The World That Day' }),
    ).toBeInTheDocument()

    // The screen still works: back to the start, then a working demo date.
    server.resetHandlers()
    await user.click(screen.getByRole('button', { name: '처음으로' }))
    await user.click(screen.getByRole('button', { name: DEMO['1995-03-15'] }))
    expect(await screen.findByText(NOTICE)).toBeInTheDocument()
  })
})

describe('privacy: the date stays in memory only (C4, C5)', () => {
  it('never reaches the address, history, browser storage or the console', async () => {
    const consoleCalls = (
      ['log', 'info', 'warn', 'error', 'debug'] as const
    ).map((method) => vi.spyOn(console, method))
    const startHref = window.location.href
    const startHistory = window.history.length
    localStorage.clear()
    sessionStorage.clear()

    const user = await openDemo('1988-09-17')
    await eventList()
    await user.click(screen.getByRole('button', { name: '처음으로' }))
    await user.click(screen.getByRole('button', { name: DEMO['2002-06-22'] }))
    await eventList()
    await user.click(screen.getByRole('button', { name: '처음으로' }))
    // An error path too: errors must not log the date either.
    server.use(
      http.get(
        '/api/story/world',
        () => new HttpResponse(null, { status: 503 }),
      ),
    )
    await user.click(screen.getByRole('button', { name: DEMO['1995-03-15'] }))
    await within(world()).findByText(ERROR)

    expect(window.location.href).toBe(startHref)
    for (const value of DATE_STRINGS) {
      expect(window.location.href).not.toContain(value)
    }
    expect(window.history.length).toBe(startHistory)
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    for (const spy of consoleCalls) {
      expect(spy).not.toHaveBeenCalled()
    }
  })
})

describe('event media and source links (CLAUDE.md 7.1)', () => {
  const base = world1988WithMedia.events[0]

  it('hides an event photo whose source is empty, with no link card in its place', async () => {
    answer({
      ...world1988,
      events: [
        {
          ...base,
          media: [{ ...base.media[0], source_name: '' }, base.media[1]],
        },
      ],
    })
    await openDemo('1988-09-17')

    const [item] = within(await eventList()).getAllByRole('listitem')

    expect(within(item).getAllByRole('figure')).toHaveLength(1)
    expect(within(item).getByText('촬영 시점 미상')).toBeInTheDocument()
    expect(
      within(item)
        .getAllByRole('img')
        .map((img) => img.getAttribute('src')),
    ).not.toContain(base.media[0].thumbnail_url)
    // Only the remaining card's "원문 보기" plus the two source line links.
    expect(
      within(item).getAllByRole('link', { name: /^원문 보기/ }),
    ).toHaveLength(1)
  })

  it('keeps a non-http source URL as plain text, never a link', async () => {
    answer({
      ...world1988,
      events: [
        {
          ...world1988.events[0],
          source_url: 'javascript:alert(1)',
          text_license_url: 'data:text/html,x',
        },
      ],
    })
    await openDemo('1988-09-17')

    const [item] = within(await eventList()).getAllByRole('listitem')

    expect(within(item).queryAllByRole('link')).toHaveLength(0)
    expect(item).toHaveTextContent('출처: 한국어 위키백과 「9월 17일」')
    expect(item).toHaveTextContent('CC BY-SA 4.0')
    expect(
      item.querySelector('[href^="javascript:"], [href^="data:"]'),
    ).toBeNull()
  })

  it('keeps the server order of normal events around a tragic one', async () => {
    const [first, second] = world1988.events
    answer({
      ...world1988,
      events: [
        first,
        {
          ...second,
          id: 'kowiki-1988-09-17-00000003',
          summary: '시험용 비극 사건 문장',
          tone: 'tragic',
          tone_basis: 'rule:keyword',
        },
        world1988.events[2],
      ],
    })
    await openDemo('1988-09-17')

    const list = await eventList()
    const items = within(list).getAllByRole('listitem')

    expect(items.map((item) => item.querySelector('p')?.textContent)).toEqual([
      first.summary,
      world1988.events[2].summary,
    ])
    expect(
      within(list).queryByText('시험용 비극 사건 문장'),
    ).not.toBeInTheDocument()
  })
})
