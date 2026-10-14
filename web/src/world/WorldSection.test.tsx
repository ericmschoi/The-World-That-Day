// Plan M3b2 T2: the "그날의 세계" section. MSW only, demo dates (product brief
// 12) plus the boundary date 2001-02-29. Every test waits for the final state.
import { render, screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse, type JsonBodyType } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Media } from '../media/types'
import { olympicsOpeningPd } from '../mocks/fixtures/world/media'
import {
  world1988,
  world1988WithMedia,
  world1995,
} from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import type { WorldEvent, WorldResponse } from './types'
import { WorldSection } from './WorldSection'

afterEach(() => {
  vi.restoreAllMocks()
})

const NOTICE = '이 날짜에 기록된 큰 사건을 찾지 못했습니다.'
const ERROR = '이 섹션을 불러오지 못했습니다.'

const answer = (body: JsonBodyType) =>
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

const region = () => screen.getByRole('region', { name: '그날의 세계' })

// Accessible name with whitespace ignored (jsdom trims the hidden span's space).
const named = (expected: string) => (name: string) =>
  name.replace(/\s/g, '') === expected.replace(/\s/g, '')

const eventList = () => screen.findByRole('list', { name: '사건 목록' })

const photo: Media = {
  ...olympicsOpeningPd,
  date_relation: { kind: 'same_day', years: 0 },
}

// Event A, plainly synthetic, built from the first 1988 event.
const eventA: WorldEvent = {
  ...world1988.events[0],
  id: 'kowiki-1988-09-17-00000001',
  title: '시험용 사건',
  summary: '시험용 사건 문장 A',
}

const tragicEvent: WorldEvent = {
  ...eventA,
  id: 'kowiki-1988-09-17-00000002',
  summary: '시험용 비극 사건 문장',
  tone: 'tragic',
  tone_basis: 'rule:keyword',
  media: [{ ...photo, id: 'example-tragic-photo' }],
}

const response = (events: WorldEvent[]): WorldResponse => ({
  ...world1988,
  events,
})

describe('WorldSection loading (T2 (1))', () => {
  it('is busy with "불러오는 중…" until the answer arrives', async () => {
    render(<WorldSection date="1988-09-17" />)

    expect(region()).toHaveAttribute('aria-busy', 'true')
    expect(region()).toHaveTextContent('불러오는 중…')

    await eventList()
    expect(region()).not.toHaveAttribute('aria-busy')
    expect(region()).not.toHaveTextContent('불러오는 중…')
  })
})

describe('WorldSection with media (default handler, T2 (2), (6))', () => {
  it('shows the three events in fixture order, text exactly as written', async () => {
    render(<WorldSection date="1988-09-17" />)

    const items = within(await eventList()).getAllByRole('listitem')

    expect(items).toHaveLength(3)
    world1988WithMedia.events.forEach((event, i) => {
      expect(
        within(items[i]).getByText(event.summary, { exact: true }),
      ).toBeInTheDocument()
    })
    expect(screen.getAllByText(/서울 하계 올림픽/)).toHaveLength(1)
  })

  it('gives each event a source line with new-tab links', async () => {
    render(<WorldSection date="1988-09-17" />)

    const items = within(await eventList()).getAllByRole('listitem')

    world1988WithMedia.events.forEach((event, i) => {
      const source = within(items[i]).getByRole('link', {
        name: named(`「${event.source_title}」 (새 창에서 열림)`),
      })
      const license = within(items[i]).getByRole('link', {
        name: named('CC BY-SA 4.0 (새 창에서 열림)'),
      })
      expect(source).toHaveAttribute('href', event.source_url)
      expect(license).toHaveAttribute('href', event.text_license_url)
      for (const link of [source, license]) {
        expect(link).toHaveAttribute('target', '_blank')
        expect(link).toHaveAttribute('rel', 'noopener noreferrer')
      }
      expect(source.closest('p')).toHaveTextContent(
        `출처: 한국어 위키백과 「${event.source_title}」`,
      )
    })
  })

  it('shows event ①’s two photos with honest date lines, and no day_media', async () => {
    render(<WorldSection date="1988-09-17" />)

    const items = within(await eventList()).getAllByRole('listitem')
    const [pd, undated] = within(items[0]).getAllByRole('figure')

    expect(screen.getAllByRole('img')).toHaveLength(2)
    expect(within(items[0]).getAllByRole('img')).toHaveLength(2)
    const pdImg = within(pd).getByRole('img')
    expect(new URL(pdImg.getAttribute('src')!).host).toBe('thumb.wikimedia.org')
    expect(within(pd).getByText('같은 날')).toBeInTheDocument()
    expect(within(undated).getByText('촬영 시점 미상')).toBeInTheDocument()
    expect(within(undated).queryByText('같은 날')).not.toBeInTheDocument()
    // day_media (the unlicensed photo and the "-day" copy) is M3b2b's.
    for (const img of screen.getAllByRole('img')) {
      expect(img.getAttribute('src')).not.toContain('example-photo-1988')
    }
    expect(screen.queryByText(/라이선스 미확인/)).not.toBeInTheDocument()
  })
})

describe('WorldSection with today’s server shape (T2 (3))', () => {
  it('shows three events and no media', async () => {
    answer(world1988)
    render(<WorldSection date="1988-09-17" />)

    const list = await eventList()

    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
    expect(screen.queryAllByRole('img')).toHaveLength(0)
    expect(screen.queryAllByRole('figure')).toHaveLength(0)
  })

  it('looks the same when the answer is partial', async () => {
    answer({ ...world1988, partial: true })
    render(<WorldSection date="1988-09-17" />)

    const list = await eventList()

    expect(within(list).getAllByRole('listitem')).toHaveLength(3)
  })
})

describe('WorldSection on a day without events (T2 (4))', () => {
  it('shows the notice and keeps the section', async () => {
    render(<WorldSection date="1995-03-15" />)

    expect(await screen.findByText(NOTICE)).toBeInTheDocument()
    expect(region()).toBeInTheDocument()
    expect(screen.queryAllByRole('img')).toHaveLength(0)
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
    expect(world1995.notice.code).toBe('NO_EVENTS_FOUND')
  })
})

describe('WorldSection leaves tragic events to M3b2b (T2 (5))', () => {
  it('shows the normal event only, with no media for the tragic one', async () => {
    answer(response([eventA, tragicEvent]))
    render(<WorldSection date="1988-09-17" />)

    const list = await eventList()

    expect(within(list).getAllByRole('listitem')).toHaveLength(1)
    expect(screen.getByText('시험용 사건 문장 A')).toBeInTheDocument()
    expect(screen.queryByText('시험용 비극 사건 문장')).not.toBeInTheDocument()
    expect(screen.queryAllByRole('img')).toHaveLength(0)
    expect(document.querySelector('iframe, video')).toBeNull()
  })

  it('shows neither notice nor list when only tragic events came', async () => {
    answer({ ...response([tragicEvent]), notice: null })
    render(<WorldSection date="1988-09-17" />)

    await waitFor(() => expect(region()).not.toHaveAttribute('aria-busy'))
    expect(screen.queryByText(NOTICE)).not.toBeInTheDocument()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
    expect(screen.queryByText(ERROR)).not.toBeInTheDocument()
    expect(screen.queryAllByRole('img')).toHaveLength(0)
  })
})

describe('WorldSection renders event text as text (T2 (7))', () => {
  it('shows tags as characters and creates no element', async () => {
    answer(response([{ ...eventA, summary: '시험용 <b>문장</b>' }]))
    const { container } = render(<WorldSection date="1988-09-17" />)

    expect(await screen.findByText('시험용 <b>문장</b>')).toBeInTheDocument()
    expect(container.querySelector('b')).toBeNull()
  })
})

describe('WorldSection errors (T2 (8))', () => {
  const expectOnlyTheErrorLine = async (dates: string[]) => {
    expect(await screen.findByText(ERROR)).toBeInTheDocument()
    const text = region().textContent ?? ''
    expect(text).toBe(`그날의 세계${ERROR}`)
    for (const value of [
      ...dates,
      'UPSTREAM_UNAVAILABLE',
      'DATE_NOT_EXIST',
      'detail',
    ]) {
      expect(text).not.toContain(value)
    }
  }

  it('503 UPSTREAM_UNAVAILABLE', async () => {
    server.use(
      http.get('/api/story/world', () =>
        HttpResponse.json(
          {
            status: 503,
            code: 'UPSTREAM_UNAVAILABLE',
            detail: 'Both source pages are unavailable.',
          },
          {
            status: 503,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    )
    render(<WorldSection date="1988-09-17" />)

    await expectOnlyTheErrorLine(['1988-09-17', 'Both source pages'])
  })

  it('400 DATE_NOT_EXIST', async () => {
    server.use(
      http.get('/api/story/world', () =>
        HttpResponse.json(
          {
            status: 400,
            code: 'DATE_NOT_EXIST',
            detail: 'date must be an existing calendar date',
          },
          {
            status: 400,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    )
    render(<WorldSection date="2001-02-29" />)

    await expectOnlyTheErrorLine(['2001-02-29', 'calendar date'])
  })

  it('timeout', async () => {
    server.use(
      http.get('/api/story/world', async () => {
        await delay('infinite')
        return HttpResponse.json(world1988)
      }),
    )
    render(<WorldSection date="1988-09-17" timeoutMs={20} />)

    await expectOnlyTheErrorLine(['1988-09-17'])
  })

  it('network error', async () => {
    server.use(http.get('/api/story/world', () => HttpResponse.error()))
    render(<WorldSection date="1988-09-17" />)

    await expectOnlyTheErrorLine(['1988-09-17'])
  })

  it('200 whose body is not the world shape', async () => {
    answer({ unexpected: true })
    render(<WorldSection date="1988-09-17" />)

    await expectOnlyTheErrorLine(['1988-09-17'])
  })

  it('200 whose body is not JSON', async () => {
    server.use(
      http.get('/api/story/world', () => HttpResponse.text('<html></html>')),
    )
    render(<WorldSection date="1988-09-17" />)

    await expectOnlyTheErrorLine(['1988-09-17'])
  })
})

describe('WorldSection cancels requests (T2 (9))', () => {
  it('aborts the request on unmount without warnings', async () => {
    const consoleError = vi.spyOn(console, 'error')
    let signal: AbortSignal | null = null
    server.use(
      http.get('/api/story/world', async ({ request }) => {
        signal = request.signal
        await delay('infinite')
        return HttpResponse.json(world1988)
      }),
    )
    const { unmount } = render(<WorldSection date="1988-09-17" />)
    await waitFor(() => expect(signal).not.toBeNull())

    unmount()

    await waitFor(() => expect(signal!.aborted).toBe(true))
    expect(consoleError).not.toHaveBeenCalled()
  })

  it('drops the old answer when the date changes', async () => {
    let releaseOld!: () => void
    const oldReleased = new Promise<void>((resolve) => {
      releaseOld = resolve
    })
    let oldSignal: AbortSignal | null = null
    server.use(
      http.get('/api/story/world', async ({ request }) => {
        const date = new URL(request.url).searchParams.get('date')
        if (date === '1988-09-17') {
          oldSignal = request.signal
          await oldReleased
          return HttpResponse.json(world1988)
        }
        return HttpResponse.json(world1995)
      }),
    )
    const { rerender } = render(<WorldSection date="1988-09-17" />)
    await waitFor(() => expect(oldSignal).not.toBeNull())

    rerender(<WorldSection date="1995-03-15" />)
    expect(await screen.findByText(NOTICE)).toBeInTheDocument()
    expect(oldSignal!.aborted).toBe(true)

    releaseOld()
    await delay(20)
    expect(screen.getByText(NOTICE)).toBeInTheDocument()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })
})

describe('WorldSection fallbackAlt (T2 (10))', () => {
  // Without descriptions the cards fall back to the alt the section passes.
  const undescribed = (event: WorldEvent): WorldEvent => ({
    ...event,
    media: event.media.map((media) => ({ ...media, description: null })),
  })

  it('names the event title and never the date', async () => {
    answer({
      ...world1988WithMedia,
      events: world1988WithMedia.events.map(undescribed),
    })
    render(<WorldSection date="1988-09-17" />)

    await eventList()
    const alts = screen
      .getAllByRole('img')
      .map((img) => img.getAttribute('alt'))

    expect(alts).toEqual([
      '「1988년 하계 올림픽」 관련 자료',
      '「1988년 하계 올림픽」 관련 자료',
    ])
    for (const alt of alts) {
      for (const word of [
        '그날',
        '같은 날',
        '당일',
        '1988-09-17',
        '1988년 9월 17일',
        '9월 17일',
      ]) {
        expect(alt).not.toContain(word)
      }
    }
  })

  it('says "사건 관련 자료" when the event has no title', async () => {
    answer(response([undescribed({ ...eventA, title: null, media: [photo] })]))
    render(<WorldSection date="1988-09-17" />)

    expect(await screen.findByRole('img')).toHaveAttribute(
      'alt',
      '사건 관련 자료',
    )
  })
})
