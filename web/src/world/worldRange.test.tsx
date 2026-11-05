// Plan C1 S3a T6 (2): an event that only starts on the requested date carries
// "Started that day — ran until <end>" outside its sentence, in the event
// list and in the tragic block (D2 (a), product brief 6.1: a range must never
// look like it all happened that day). `day`, a missing or an unknown
// `date_kind` gets no note. Synthetic English events on the demo date
// 1988-02-13 (plan D9); the cross-year range is a synthetic boundary value.
import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse, type JsonBodyType } from 'msw'
import { describe, expect, it } from 'vitest'
import { olympicsOpeningPd } from '../mocks/fixtures/world/media'
import { world1988 } from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import { rangeNote } from './rangeText'
import type { WorldEvent, WorldResponse } from './types'
import { WorldSection } from './WorldSection'

const DATE = '1988-02-13'
const NOTE = 'Started that day — ran until February 28'

const base: WorldEvent = {
  ...world1988.events[0],
  id: 'enwiki-1988-02-13-0000c001',
  date: DATE,
  date_kind: 'day',
  range_end: null,
  title: 'Test event',
  summary: 'Test day event sentence',
  source_title: 'February 13',
  source_url: 'https://en.wikipedia.org/wiki/February_13',
  media: [],
}

const rangeEvent: WorldEvent = {
  ...base,
  id: 'enwiki-1988-02-13-0000c002',
  date_kind: 'range_start',
  range_end: '1988-02-28',
  summary: 'Test range event sentence',
  source_title: '1988',
  source_url: 'https://en.wikipedia.org/wiki/1988',
  media: [
    { ...olympicsOpeningPd, date_relation: { kind: 'same_day', years: 0 } },
  ],
}

const tragicRange: WorldEvent = {
  ...rangeEvent,
  id: 'enwiki-1988-02-13-0000c003',
  summary: 'Test tragic range event sentence',
  tone: 'tragic',
  tone_basis: 'rule:keyword',
  media: [],
}

const tragicDay: WorldEvent = {
  ...tragicRange,
  id: 'enwiki-1988-02-13-0000c004',
  date_kind: 'day',
  range_end: null,
  summary: 'Test tragic day event sentence',
}

const response = (events: unknown[]) => ({
  date: DATE,
  events,
  day_media: [],
  notice: null,
  partial: false,
})

const answer = (body: JsonBodyType) =>
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

const eventItems = async () =>
  within(await screen.findByRole('list', { name: 'Events' })).getAllByRole(
    'listitem',
  )

const tragicBlock = async () =>
  (
    await screen.findByRole('heading', {
      name: 'Difficult events on the same day',
    })
  ).parentElement as HTMLElement

describe('rangeNote', () => {
  it('gives the end as month and day when it is in the event year', () => {
    expect(rangeNote(rangeEvent)).toBe(NOTE)
  })

  it('adds the year when the range ends in a later year', () => {
    expect(
      rangeNote({
        date: '1987-12-30',
        date_kind: 'range_start',
        range_end: '1988-01-02',
      }),
    ).toBe('Started that day — ran until January 2, 1988')
  })

  it('says only "Started that day" when the end cannot be read', () => {
    for (const range_end of [
      null,
      '',
      '1988-02-30',
      '1988-2-28',
      '1988-02-13',
      '1988-02-12',
      '28 February 1988',
    ]) {
      expect(
        rangeNote({ date: DATE, date_kind: 'range_start', range_end }),
        String(range_end),
      ).toBe('Started that day')
    }
  })

  it('is null for day, missing and unknown date_kind values', () => {
    const kinds: unknown[] = ['day', undefined, null, 'range', 'RANGE_START']
    for (const date_kind of kinds) {
      expect(
        rangeNote({
          date: DATE,
          date_kind: date_kind as WorldEvent['date_kind'],
          range_end: '1988-02-28',
        }),
        String(date_kind),
      ).toBeNull()
    }
  })
})

describe('range note in the event list', () => {
  it('follows the sentence in its own paragraph, the sentence unchanged', async () => {
    answer(response([base, rangeEvent]))
    render(<WorldSection date={DATE} />)
    const [day, range] = await eventItems()

    const sentence = within(range).getByText(rangeEvent.summary)
    const note = within(range).getByText(NOTE)
    expect(sentence.tagName).toBe('P')
    expect(sentence.textContent).toBe('Test range event sentence')
    expect(note.tagName).toBe('P')
    expect(sentence.contains(note)).toBe(false)
    expect(sentence.nextElementSibling).toBe(note)
    // The note is our page text, not Wikipedia's.
    expect(note).not.toHaveAttribute('lang')
    // Sentence, note, the photo, then the source line.
    const order = [...range.children].map((child) => child.tagName)
    expect(order).toEqual(['P', 'P', 'FIGURE', 'P'])
    expect(range.lastElementChild).toHaveTextContent(
      'Source: English Wikipedia, “1988”',
    )

    expect(day).not.toHaveTextContent(/Started that day/)
    expect([...day.children].map((child) => child.tagName)).toEqual(['P', 'P'])
  })

  it('shows nothing for a missing or an unknown date_kind', async () => {
    const withoutKind = Object.fromEntries(
      Object.entries(rangeEvent).filter(([key]) => key !== 'date_kind'),
    )
    answer(
      response([
        { ...withoutKind, id: 'enwiki-1988-02-13-0000c005' },
        { ...rangeEvent, id: 'enwiki-1988-02-13-0000c006', date_kind: 'span' },
      ]),
    )
    render(<WorldSection date={DATE} />)
    const items = await eventItems()

    expect(items).toHaveLength(2)
    for (const item of items) {
      expect(within(item).getByText(rangeEvent.summary)).toBeInTheDocument()
      expect(item).not.toHaveTextContent(/Started that day/)
    }
  })

  it('appears once per range event on the default 1988-09-17 fixture', async () => {
    answer(world1988 satisfies WorldResponse)
    render(<WorldSection date="1988-09-17" />)
    const items = await eventItems()

    expect(screen.getAllByText(/Started that day/)).toHaveLength(1)
    expect(
      within(items[2]).getByText('Started that day — ran until October 2'),
    ).toBeInTheDocument()
  })
})

describe('range note in the tragic block', () => {
  it('follows the sentence there too, and only for the range event', async () => {
    answer(response([base, tragicRange, tragicDay]))
    render(<WorldSection date={DATE} />)
    const block = await tragicBlock()
    const [range, day] = within(block).getAllByRole('listitem')

    const sentence = within(range).getByText(tragicRange.summary)
    const note = within(range).getByText(NOTE)
    expect(sentence.textContent).toBe('Test tragic range event sentence')
    expect(sentence.nextElementSibling).toBe(note)
    expect(note.tagName).toBe('P')
    expect(note).not.toHaveAttribute('lang')
    expect(day).not.toHaveTextContent(/Started that day/)

    // The normal list above has no note.
    const [normal] = await eventItems()
    expect(normal).not.toHaveTextContent(/Started that day/)
    expect(screen.getAllByText(/Started that day/)).toHaveLength(1)
  })
})
