// Plan M3b2b T1: day_media in three groups (M3-P17 (a), decision 4 (a)) and
// the tragic block (product brief 6.3, #22 (c)). MSW only, demo dates.
import { render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse, type JsonBodyType } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Media } from '../media/types'
import {
  captureTimeUnknown,
  olympicsOpeningPd,
} from '../mocks/fixtures/world/media'
import {
  world1988,
  world1988WithMedia,
  worldTragicOnly,
  worldWithTragic,
} from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import { WorldSection } from './WorldSection'

afterEach(() => {
  vi.restoreAllMocks()
})

const SAME_DAY = '그날 찍힌 사진'
const OTHER_TIME = '다른 때 찍힌 사진'
const UNKNOWN = '촬영 시점을 확인하지 못한 사진'
const TRAGIC = '같은 날 있었던 아픈 일'
const NOTICE = '이 날짜에 기록된 큰 사건을 찾지 못했습니다.'
const DATE_WORDS = [
  '그날',
  'Same day',
  '당일',
  '1988-09-17',
  '1988년 9월 17일',
  '9월 17일',
]

const answer = (body: JsonBodyType) =>
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

const region = () => screen.getByRole('region', { name: '그날의 세계' })

async function renderReady(date = '1988-09-17') {
  render(<WorldSection date={date} />)
  await waitFor(() => expect(region()).not.toHaveAttribute('aria-busy'))
}

const h3s = () =>
  within(region())
    .queryAllByRole('heading', { level: 3 })
    .map((heading) => heading.textContent)

// The block that a subheading opens.
const block = (title: string) =>
  within(region()).getByRole('heading', { level: 3, name: title })
    .parentElement!

const srcs = (element: HTMLElement) =>
  within(element)
    .queryAllByRole('img')
    .map((img) => img.getAttribute('src'))

// Synthetic day_media with other capture times, from the PD photo.
const sameMonth: Media = {
  ...olympicsOpeningPd,
  id: 'example-same-month',
  thumbnail_url: 'https://thumb.wikimedia.org/example-same-month.jpg',
  captured_at: '1988-09',
  date_precision: 'month',
  date_relation: { kind: 'same_month', years: 0 },
}
const yearsApart: Media = {
  ...olympicsOpeningPd,
  id: 'example-years-apart',
  thumbnail_url: 'https://thumb.wikimedia.org/example-years-apart.jpg',
  captured_at: '1985-05-01',
  date_precision: 'day',
  date_relation: { kind: 'years_apart', years: -3 },
}
const sameDay: Media = {
  ...olympicsOpeningPd,
  id: 'example-same-day',
  date_relation: { kind: 'same_day', years: 0 },
}
const undated: Media = {
  ...captureTimeUnknown,
  id: 'example-undated',
  date_relation: { kind: 'unknown', years: 0 },
}

describe('demo 1988-09-17 day_media groups (T1 (1))', () => {
  it('puts the same-day photo and the undated copy under their own headings', async () => {
    await renderReady()

    expect(h3s()).toEqual([SAME_DAY, UNKNOWN])
    const day = block(SAME_DAY)
    const unknown = block(UNKNOWN)
    const [licenseUnknownPhoto, undatedCopy] = world1988WithMedia.day_media

    expect(srcs(day)).toEqual([licenseUnknownPhoto.thumbnail_url])
    expect(within(day).getByText('Same day')).toBeInTheDocument()
    expect(within(day).getByText('Licence not verified')).toBeInTheDocument()
    expect(
      within(day).queryByText('Capture time unknown'),
    ).not.toBeInTheDocument()
    expect(srcs(unknown)).toEqual([undatedCopy.thumbnail_url])
    expect(
      within(unknown).getByText('Capture time unknown'),
    ).toBeInTheDocument()
    expect(within(unknown).queryByText('Same day')).not.toBeInTheDocument()
    // Event ①'s two photos stay inside the event, four img in all.
    expect(screen.getAllByRole('img')).toHaveLength(4)
    expect(screen.queryByText(TRAGIC)).not.toBeInTheDocument()
  })

  it('keeps the groups after the event list', async () => {
    await renderReady()

    const list = screen.getByRole('list', { name: '사건 목록' })
    const heading = within(region()).getByRole('heading', { name: SAME_DAY })

    expect(
      list.compareDocumentPosition(heading) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
  })
})

describe('days without day_media (T1 (1))', () => {
  it('2002-06-22: no group heading', async () => {
    await renderReady('2002-06-22')

    expect(screen.getByRole('list', { name: '사건 목록' })).toBeInTheDocument()
    expect(h3s()).toEqual([])
  })

  it('1995-03-15: the notice only, no group heading and no tragic block', async () => {
    await renderReady('1995-03-15')

    expect(screen.getByText(NOTICE)).toBeInTheDocument()
    expect(h3s()).toEqual([])
  })

  it('today’s server shape: no group heading', async () => {
    answer(world1988)
    await renderReady()

    expect(h3s()).toEqual([])
  })
})

describe('day_media group rules (T1 (2))', () => {
  it('orders the three h3 headings and keeps server order inside each', async () => {
    answer({
      ...world1988,
      day_media: [undated, yearsApart, sameDay, sameMonth],
    })
    await renderReady()

    expect(h3s()).toEqual([SAME_DAY, OTHER_TIME, UNKNOWN])
    expect(srcs(block(SAME_DAY))).toEqual([sameDay.thumbnail_url])
    expect(srcs(block(OTHER_TIME))).toEqual([
      yearsApart.thumbnail_url,
      sameMonth.thumbnail_url,
    ])
    expect(srcs(block(UNKNOWN))).toEqual([undated.thumbnail_url])
  })

  it('shows other capture times with their real badge, under "다른 때 찍힌 사진" only', async () => {
    answer({ ...world1988, day_media: [sameMonth, yearsApart] })
    await renderReady()

    expect(h3s()).toEqual([OTHER_TIME])
    const other = block(OTHER_TIME)
    expect(within(other).getByText('Same month')).toBeInTheDocument()
    expect(within(other).getByText('3 years earlier')).toBeInTheDocument()
    expect(within(other).queryByText('Same day')).not.toBeInTheDocument()
  })

  it('keeps the undated photo under "촬영 시점을 확인하지 못한 사진" only', async () => {
    answer({ ...world1988, day_media: [undated] })
    await renderReady()

    expect(h3s()).toEqual([UNKNOWN])
  })

  it('never puts a same_day relation without a day capture under "그날 찍힌 사진"', async () => {
    // The caption falls back to "Capture time unknown", so the heading
    // follows it.
    answer({
      ...world1988,
      day_media: [
        { ...sameMonth, date_relation: { kind: 'same_day', years: 0 } },
      ],
    })
    await renderReady()

    expect(h3s()).toEqual([UNKNOWN])
    expect(
      within(block(UNKNOWN)).getByText('Capture time unknown'),
    ).toBeVisible()
  })

  it('shows no heading for media that cannot be shown', async () => {
    answer({
      ...world1988,
      day_media: [{ ...sameDay, source_name: '' }],
    })
    await renderReady()

    expect(h3s()).toEqual([])
    expect(screen.queryAllByRole('img')).toHaveLength(0)
  })
})

describe('tragic block (T1 (1), (3))', () => {
  const tragicBlock = () => block(TRAGIC)

  it('worldTragicOnly: the block only, never its media', async () => {
    answer(worldTragicOnly)
    await renderReady()

    expect(h3s()).toEqual([TRAGIC])
    expect(
      within(tragicBlock()).getByText('시험용 비극 사건 문장'),
    ).toBeInTheDocument()
    expect(worldTragicOnly.events[0].media).toHaveLength(1)
    expect(screen.queryAllByRole('img')).toHaveLength(0)
    expect(region().querySelector('img, iframe, video')).toBeNull()
    expect(screen.queryByText(NOTICE)).not.toBeInTheDocument()
    expect(
      screen.queryByRole('list', { name: '사건 목록' }),
    ).not.toBeInTheDocument()
  })

  it('worldWithTragic: the normal event first, the block after it', async () => {
    answer(worldWithTragic)
    await renderReady()

    const list = screen.getByRole('list', { name: '사건 목록' })
    const normal = screen.getByText('시험용 사건 문장 A')
    const tragic = screen.getByText('시험용 비극 사건 문장')

    expect(list).toContainElement(normal)
    expect(tragicBlock()).toContainElement(tragic)
    expect(list).not.toContainElement(tragic)
    expect(
      normal.compareDocumentPosition(tragic) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
    expect(region().querySelector('img, iframe, video')).toBeNull()
  })

  it('gives the source line two new-tab links', async () => {
    answer(worldWithTragic)
    await renderReady()

    const [event] = worldTragicOnly.events
    const links = within(tragicBlock()).getAllByRole('link')

    expect(links).toHaveLength(2)
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      event.source_url,
      event.text_license_url,
    ])
    for (const link of links) {
      expect(link).toHaveAccessibleName(/opens in a new tab/)
      expect(link).toHaveAttribute('target', '_blank')
      expect(link).toHaveAttribute('rel', 'noopener noreferrer')
    }
    expect(links[0].closest('p')).toHaveTextContent(
      `출처: 한국어 위키백과 「${event.source_title}」`,
    )
  })
})

describe('day_media alt text (T1 (5))', () => {
  const undescribed = (media: Media): Media => ({ ...media, description: null })

  it('is "기록 사진" for day_media, never a date word', async () => {
    answer({
      ...world1988WithMedia,
      events: world1988WithMedia.events.map((event) => ({
        ...event,
        media: event.media.map(undescribed),
      })),
      day_media: [
        ...world1988WithMedia.day_media.map(undescribed),
        undescribed(sameMonth),
      ],
    })
    await renderReady()

    const alts = screen
      .getAllByRole('img')
      .map((img) => img.getAttribute('alt') ?? '')
    const dayAlts = [SAME_DAY, OTHER_TIME, UNKNOWN].flatMap((title) =>
      within(block(title))
        .getAllByRole('img')
        .map((img) => img.getAttribute('alt')),
    )

    expect(alts).toHaveLength(5)
    expect(dayAlts).toEqual(['기록 사진', '기록 사진', '기록 사진'])
    for (const alt of alts) {
      for (const word of DATE_WORDS) expect(alt).not.toContain(word)
    }
  })
})

describe('shared link parts (T1 (6))', () => {
  const sources = import.meta.glob<string>('/src/**/*.tsx', {
    query: '?raw',
    import: 'default',
    eager: true,
  })
  const product = Object.entries(sources).filter(
    ([path]) => !path.includes('.test.'),
  )

  it('defines NewTabLink in one shared place only', () => {
    const defining = product
      .filter(([, text]) => /function NewTabLink\b/.test(text))
      .map(([path]) => path)
    expect(defining).toEqual(['/src/components/NewTabLink.tsx'])
  })

  it('uses the same source line in the event list and the tragic block', () => {
    for (const path of [
      '/src/world/WorldEventItem.tsx',
      '/src/world/TragicEvents.tsx',
    ]) {
      expect(sources[path]).toContain(
        "import { SourceLine } from './SourceLine'",
      )
      expect(sources[path]).not.toContain('NewTabLink')
    }
  })
})
