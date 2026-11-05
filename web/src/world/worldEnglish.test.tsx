// Plan C1 S1 T6 (2): the screen text that we write is English (en-CA). A
// synthetic English world answer is drawn with every caption variant, and
// the page text must hold no Hangul and no US "license" spelling. The input
// screen is left out on purpose (S1-P1 (a): its temporary buttons stay Korean
// until S2). Server-supplied Korean event text is out of scope until S3a, so
// every event and media text below is plainly fake English. Dates are the
// demo date 1988-09-17 (product brief 12) and nearby years.
import { render, screen, waitFor } from '@testing-library/react'
import { delay, http, HttpResponse, type JsonBodyType } from 'msw'
import { describe, expect, it } from 'vitest'
import { SiteFooter } from '../components/SiteFooter'
import { ConsentNotice } from '../consent/ConsentNotice'
import { ConsentProvider } from '../consent/ConsentProvider'
import type { DatePrecision, DateRelation, Media } from '../media/types'
import { olympicsOpeningPd } from '../mocks/fixtures/world/media'
import { world1995 } from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import { StoryScreen } from '../screens/StoryScreen'
import type { WorldEvent, WorldResponse } from './types'
import { WorldSection } from './WorldSection'

// Korean Hangul: Jamo, Compatibility Jamo and the syllable block.
const HANGUL = /[ᄀ-ᇿ㄰-㆏가-힯]/
const US_SPELLING = /\blicense\b/i

const answer = (body: JsonBodyType) =>
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

// Text a screen reader or a sighted reader gets from our own words: the page
// text plus the attributes that are read aloud.
function everythingRead(): string {
  const attributes = [
    ...document.body.querySelectorAll('[alt], [aria-label], [title]'),
  ].flatMap((element) =>
    ['alt', 'aria-label', 'title']
      .map((name) => element.getAttribute(name))
      .filter((value): value is string => value !== null),
  )
  return [document.body.textContent ?? '', ...attributes].join('\n')
}

function expectEnglishOnly(text: string) {
  expect(text).not.toMatch(HANGUL)
  expect(text).not.toMatch(US_SPELLING)
}

let sequence = 0
// A fake photo. Nothing in it is Korean and none of the text names a licence,
// so the caption has to supply its own wording.
function photo(
  kind: DateRelation['kind'],
  years: number,
  capturedAt: string | null,
  precision: DatePrecision | null,
  extra: Partial<Media> = {},
): Media {
  sequence += 1
  return {
    ...olympicsOpeningPd,
    id: `example-english-${sequence}`,
    url: `https://media.example.org/files/example-${sequence}.jpg`,
    thumbnail_url: `https://media.example.org/thumbs/example-${sequence}.jpg`,
    source_name: 'Example Archive',
    source_url: `https://archive.example.org/items/example-${sequence}`,
    license: 'CC BY 4.0',
    license_status: 'free',
    license_url: 'https://creativecommons.org/licenses/by/4.0/',
    attribution: 'Example Photographer · Example Archive',
    description: `Test photo ${sequence}`,
    captured_at: capturedAt,
    date_precision: precision,
    date_relation: { kind, years },
    ...extra,
  }
}

// Every badge kind, with the singular and plural of "year", both directions,
// and each capture-time precision.
const sameDay = photo('same_day', 0, '1988-09-17', 'day')
const sameMonth = photo('same_month', 0, '1988-09', 'month')
const sameYear = photo('same_year', 0, '1988', 'year')
const threeEarlier = photo('years_apart', -3, '1985-05-01', 'day')
const oneEarlier = photo('years_apart', -1, '1987-02', 'month')
const oneLater = photo('years_apart', 1, '1989', 'year')
const threeLater = photo('years_apart', 3, '1991-12-31', 'day')
const aroundSame = photo('approximate', 0, '1988', 'approx')
const aboutEarlier = photo('approximate', -2, '1986', 'approx')
const aboutOneLater = photo('approximate', 1, '1989', 'approx')
const present = photo('current', 0, null, 'current')
const captureUnknown = photo('unknown', 0, null, null)
const licenceUnknown = photo('same_day', 0, '1988-09-17', 'day', {
  id: 'example-english-licence-unknown',
  license: null,
  license_status: 'unknown',
  license_url: null,
})
const providerTerms = photo('same_month', 0, '1988-09', 'month', {
  id: 'example-english-provider-terms',
  license: 'provider-terms',
  license_status: 'other',
  license_url: 'https://example.org/terms',
})
// No description: the figure falls back to the event or archive alt text.
const noDescription = photo('years_apart', -3, '1985-05-01', 'day', {
  id: 'example-english-no-description',
  description: null,
})
const eventPhotoNoDescription = photo('same_day', 0, '1988-09-17', 'day', {
  id: 'example-english-event-no-description',
  description: null,
})
const eventPhotoUnknown = photo('unknown', 0, null, null, {
  id: 'example-english-event-unknown',
})

const normalEvent: WorldEvent = {
  id: 'example-english-event-normal',
  date: '1988-09-17',
  scope: 'world',
  title: 'Test event',
  summary: 'Test event sentence A',
  summary_lang: 'ko', // The server contract; the fake text is English.
  summary_modified: false,
  text_license: 'CC BY-SA 4.0',
  text_license_url: 'https://creativecommons.org/licenses/by-sa/4.0/',
  source_name: 'Example Encyclopedia',
  source_title: 'September 17',
  source_url: 'https://encyclopedia.example.org/wiki/September_17',
  source_section: '사건',
  source_revision: 1,
  wikidata_qid: null,
  tone: 'normal',
  tone_basis: 'curated',
  related_links: [],
  media: [eventPhotoNoDescription, eventPhotoUnknown],
  retrieved_at: '2026-10-12T03:00:00Z',
}

const untitledEvent: WorldEvent = {
  ...normalEvent,
  id: 'example-english-event-untitled',
  title: null,
  summary: 'Test event sentence B',
  media: [photo('same_day', 0, '1988-09-17', 'day', { description: null })],
}

const tragicEvent: WorldEvent = {
  ...normalEvent,
  id: 'example-english-event-tragic',
  summary: 'Test tragic event sentence',
  tone: 'tragic',
  tone_basis: 'rule:keyword',
  media: [photo('same_day', 0, '1988-09-17', 'day')],
}

const englishWorld: WorldResponse = {
  date: '1988-09-17',
  events: [normalEvent, untitledEvent, tragicEvent],
  day_media: [
    sameDay,
    licenceUnknown,
    sameMonth,
    sameYear,
    threeEarlier,
    oneEarlier,
    oneLater,
    threeLater,
    aroundSame,
    aboutEarlier,
    aboutOneLater,
    present,
    providerTerms,
    noDescription,
    captureUnknown,
  ],
  notice: null,
  partial: false,
}

describe('screen text is English (plan C1 S1 T6 (2))', () => {
  it('draws a full synthetic world answer with no Hangul and no "license"', async () => {
    answer(englishWorld)
    render(
      <>
        <StoryScreen date="1988-09-17" onRestart={() => {}} />
        <SiteFooter />
      </>,
    )
    await screen.findByRole('list', { name: 'Events' })
    await waitFor(() =>
      expect(
        screen.getByRole('region', { name: 'The world that day' }),
      ).not.toHaveAttribute('aria-busy'),
    )

    const text = document.body.textContent ?? ''
    expectEnglishOnly(text)
    expectEnglishOnly(everythingRead())

    // Section titles, the other four sections' status, the button, the footer.
    for (const title of [
      'Opening',
      'The neighbourhood that day',
      'Scenes of the city',
      'The world that day',
      'Closing',
    ]) {
      expect(screen.getByRole('region', { name: title })).toBeInTheDocument()
    }
    expect(text.match(/This section is coming soon\./g)).toHaveLength(4)
    expect(screen.getByRole('button', { name: 'Start over' })).toBeVisible()
    expect(text).toContain('Privacy policy')

    // Event list, source lines and the tragic block.
    expect(text).toContain('Test event sentence A')
    expect(text).toContain('Source: Example Encyclopedia, ')
    expect(text).toContain('“September 17”')
    expect(text).toContain('Difficult events on the same day')

    // The three day-media groups.
    for (const heading of [
      'Photos taken that day',
      'Photos taken at other times',
      'Photos with an unknown capture time',
    ]) {
      expect(
        screen.getByRole('heading', { level: 3, name: heading }),
      ).toBeInTheDocument()
    }

    // Every badge kind, both directions, singular and plural.
    for (const badge of [
      'Same day',
      'Same month',
      'Same year',
      '3 years earlier',
      '1 year earlier',
      '1 year later',
      '3 years later',
      'Around the same year',
      'About 2 years earlier',
      'About 1 year later',
      'Present day',
    ]) {
      expect(text, `badge "${badge}"`).toContain(badge)
    }

    // Capture-time text for each precision, approximate, current, unknown.
    for (const taken of [
      'Taken September 17, 1988',
      'Taken September 1988',
      'Taken 1988',
      'Taken May 1, 1985',
      'Taken around 1986 (estimated)',
      'Capture time not provided',
      'Capture time unknown',
    ]) {
      expect(text, `text "${taken}"`).toContain(taken)
    }

    // Licence wording, links and the hidden new-tab hint (en-CA spelling).
    expect(text).toContain('Licence not verified')
    expect(text).toContain("Provider's terms apply")
    expect(text).toContain('View original')
    expect(text).toContain('Licence')
    expect(text).toContain('Terms')
    expect(text).toContain('(opens in a new tab)')
    expect(
      screen.getAllByRole('link', { name: /^Licence/ }).length,
    ).toBeGreaterThan(0)
    expect(
      screen.getAllByRole('link', { name: /^Terms/ }).length,
    ).toBeGreaterThan(0)

    // Alt text fallbacks (not part of textContent).
    const alts = [...document.body.querySelectorAll('img')].map((image) =>
      image.getAttribute('alt'),
    )
    expect(alts).toContain('Material about “Test event”')
    expect(alts).toContain('Material about this event')
    expect(alts).toContain('Archive photo')
  })

  it('draws the "no major events" notice and the coming-soon sections', async () => {
    answer(world1995)
    render(<StoryScreen date="1995-03-15" onRestart={() => {}} />)
    await screen.findByText('No major events were found for this date.')

    const text = document.body.textContent ?? ''
    expectEnglishOnly(text)
    expectEnglishOnly(everythingRead())
    expect(text.match(/This section is coming soon\./g)).toHaveLength(4)
  })

  it('draws the loading state', async () => {
    server.use(
      http.get('/api/story/world', async () => {
        await delay('infinite')
        return HttpResponse.json(englishWorld)
      }),
    )
    render(<WorldSection date="1988-09-17" />)
    await screen.findByText('Loading…')

    expectEnglishOnly(everythingRead())
    expect(document.body.textContent).toContain('The world that day')
  })

  it('draws the failed state without any Korean', async () => {
    server.use(
      http.get('/api/story/world', () =>
        HttpResponse.json({ code: 'UPSTREAM_UNAVAILABLE' }, { status: 500 }),
      ),
    )
    render(<WorldSection date="1988-09-17" />)
    await screen.findByText("This section couldn't be loaded.")

    expectEnglishOnly(everythingRead())
  })

  it('draws the consent notice in English (the part the input screen shares)', () => {
    render(
      <ConsentProvider>
        <ConsentNotice hint />
      </ConsentProvider>,
    )
    const text = everythingRead()
    expectEnglishOnly(text)
    expect(text).toContain('I agree to the privacy policy (required)')
    expect(text).toContain('Read the privacy policy')
    expect(text).toContain('YouTube Terms of Service')
    expect(text).toContain('Agree to the privacy policy to see the story.')
  })
})
