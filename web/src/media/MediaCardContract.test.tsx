import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  koglPhoto,
  sameDayPhoto,
  undatedPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import { MediaItem } from './MediaItem'
import type { Media } from './types'

// M3b1b T3: the media contract checked through the card entry point
// (`MediaItem`), so the rules hold for what the user actually sees, not only
// for the helper functions. Requested dates are the demo inputs (brief 12).

const FALLBACK = '서울 사진'

const patch = (media: Media, fields: Record<string, unknown>) =>
  ({ ...media, ...fields }) as Media

const renderItem = (media: Media) =>
  render(<MediaItem media={media} fallbackAlt={FALLBACK} />)

const firstLine = (figure: HTMLElement) =>
  figure.querySelector('figcaption > p') as HTMLElement

// Words and dates that would make a photo look like it is from that day.
const THAT_DAY_HINTS = [
  'Same day',
  'Same month',
  'Same year',
  'that day',
  'Around',
  'About',
]

describe('honest date boundaries on the card (demo 1988-09-17)', () => {
  it.each([
    // [case, date_relation, date_precision, captured_at, badge, first line]
    [
      'same day',
      'same_day',
      0,
      'day',
      '1988-09-17',
      'Same day',
      'Same day Taken September 17, 1988',
    ],
    [
      'same month, other day',
      'same_month',
      0,
      'day',
      '1988-09-02',
      'Same month',
      'Same month Taken September 2, 1988',
    ],
    [
      'same month, month precision',
      'same_month',
      0,
      'month',
      '1988-09',
      'Same month',
      'Same month Taken September 1988',
    ],
    [
      'same year, other month',
      'same_year',
      0,
      'month',
      '1988-03',
      'Same year',
      'Same year Taken March 1988',
    ],
    [
      'same year, year precision',
      'same_year',
      0,
      'year',
      '1988',
      'Same year',
      'Same year Taken 1988',
    ],
    [
      'same year, leap day',
      'same_year',
      0,
      'day',
      '1988-02-29',
      'Same year',
      'Same year Taken February 29, 1988',
    ],
    [
      'different year, before',
      'years_apart',
      -1,
      'year',
      '1987',
      '1 year earlier',
      '1 year earlier Taken 1987',
    ],
    [
      'different year, after',
      'years_apart',
      2,
      'day',
      '1990-09-17',
      '2 years later',
      '2 years later Taken September 17, 1990',
    ],
    [
      'approximate, same year',
      'approximate',
      0,
      'approx',
      '1988',
      'Around the same year',
      'Around the same year Taken around 1988 (estimated)',
    ],
    [
      'approximate, after',
      'approximate',
      3,
      'approx',
      '1991',
      'About 3 years later',
      'About 3 years later Taken around 1991 (estimated)',
    ],
  ])(
    '%s: badge and capture time match',
    (_case, kind, years, date_precision, captured_at, badge, line) => {
      renderItem(
        patch(sameDayPhoto, {
          date_relation: { kind, years },
          date_precision,
          captured_at,
        }),
      )
      const figure = screen.getByRole('figure')
      const first = firstLine(figure)
      expect(first).toHaveTextContent(line)
      expect(within(first).getByText(badge)).toBeInTheDocument()
    },
  )

  // A relation the capture time cannot support must not reach the screen.
  it.each([
    ['capture date unknown', 'same_day', 0, null, null],
    [
      'same_day claimed with month precision',
      'same_day',
      0,
      'month',
      '1988-09',
    ],
    ['same_month claimed with year precision', 'same_month', 0, 'year', '1988'],
    ['same_day claimed with no capture date', 'same_day', 0, 'day', null],
    [
      'captured_at shaped wrong for day precision',
      'same_day',
      0,
      'day',
      '1988-09',
    ],
    ['non-existent leap day', 'years_apart', -1, 'day', '1987-02-29'],
    ['non-existent date', 'same_month', 0, 'day', '1988-09-31'],
    ['years_apart of zero', 'years_apart', 0, 'year', '1988'],
    [
      'approximate year with exact precision',
      'approximate',
      -1,
      'year',
      '1987',
    ],
  ])(
    '%s: shown, labeled only Capture time unknown',
    (_case, kind, years, date_precision, captured_at) => {
      renderItem(
        patch(sameDayPhoto, {
          date_relation: { kind, years },
          date_precision,
          captured_at,
        }),
      )
      const figure = screen.getByRole('figure')
      expect(within(figure).getByRole('img')).toBeInTheDocument()
      expect(firstLine(figure)).toHaveTextContent(/^Capture time unknown$/)
      expect(firstLine(figure).querySelector('span')).toBeNull()
      expect(figure.textContent).not.toContain('September 17, 1988')
      for (const hint of THAT_DAY_HINTS) {
        expect(figure.textContent).not.toContain(hint)
      }
    },
  )
})

describe('unknown capture time is never presented as that day', () => {
  // Undated variants of a photo for each demo input with media.
  it.each([
    ['1988-09-17', undatedPhoto, 'September 17, 1988'],
    [
      '2002-06-22',
      patch(koglPhoto, {
        captured_at: null,
        date_precision: null,
        date_relation: { kind: 'unknown', years: 0 },
      }),
      'June 22, 2002',
    ],
  ])('demo %s: image shown, no badge, no requested date', (_d, media, day) => {
    renderItem(media)
    const figure = screen.getByRole('figure')
    expect(within(figure).getByRole('img')).toHaveAttribute(
      'src',
      media.thumbnail_url,
    )
    expect(firstLine(figure)).toHaveTextContent(/^Capture time unknown$/)
    expect(
      figure.querySelector(
        'figcaption span:not([aria-hidden]):not(.visually-hidden)',
      ),
    ).toBeNull()
    expect(figure.textContent).not.toContain(day)
    for (const hint of THAT_DAY_HINTS) {
      expect(figure.textContent).not.toContain(hint)
    }
  })

  it('ignores a leftover captured_at when the precision is unknown', () => {
    renderItem(patch(undatedPhoto, { captured_at: '1988-09-17' }))
    const figure = screen.getByRole('figure')
    expect(firstLine(figure)).toHaveTextContent(/^Capture time unknown$/)
    expect(figure.textContent).not.toContain('September 17, 1988')
  })

  // With no description the alt text is the caller's fallbackAlt, so the
  // wording callers pass (M3b2 onward) must not hint at that day either.
  it.each([
    ['null', null],
    ['blank', '   '],
  ])(
    'undated photo with a %s description: alt has no that-day hint',
    (_c, description) => {
      renderItem(patch(undatedPhoto, { description }))
      const alt = screen.getByRole('img').getAttribute('alt') ?? ''
      expect(alt).toBe(FALLBACK)
      expect(alt).not.toContain('September 17, 1988')
      for (const hint of THAT_DAY_HINTS) {
        expect(alt).not.toContain(hint)
      }
    },
  )
})

describe('inline display, never a link card or a rehosted file', () => {
  it.each([sameDayPhoto, undatedPhoto, koglPhoto, unlicensedPhoto])(
    '$id: the photo is inline, from its original URL, outside any link',
    (media) => {
      renderItem(media)
      const figure = screen.getByRole('figure')
      const img = within(figure).getByRole('img')
      expect(img.getAttribute('src')).toBe(media.thumbnail_url)
      expect(img.closest('a')).toBeNull()
      // The only links are the caption's View original and Licence.
      for (const link of within(figure).getAllByRole('link')) {
        expect(link.closest('figcaption')).not.toBeNull()
        expect(link).toHaveAccessibleName(
          /^(View original|Licence)\s*\(opens in a new tab\)$/,
        )
      }
    },
  )
})

describe('source, license and capture time are always on the card', () => {
  it.each([sameDayPhoto, undatedPhoto, koglPhoto, unlicensedPhoto])(
    '$id: caption shows the source text and the View original link',
    (media) => {
      renderItem(media)
      const figure = screen.getByRole('figure')
      expect(figure).toHaveTextContent(media.attribution)
      expect(
        within(figure).getByRole('link', { name: /^View original/ }),
      ).toHaveAttribute('href', media.source_url)
      expect(firstLine(figure).textContent).not.toBe('')
    },
  )

  it.each([
    ['license_status unknown with a name', { license_status: 'unknown' }],
    ['license null', { license: null }],
    ['license blank', { license: '   ' }],
  ])('%s: Licence not verified, image still shown', (_case, fields) => {
    renderItem(patch(sameDayPhoto, fields))
    const figure = screen.getByRole('figure')
    expect(within(figure).getByRole('img')).toBeInTheDocument()
    expect(within(figure).getByText('Licence not verified')).toBeInTheDocument()
  })

  it.each([
    ['source_name', '   '],
    ['attribution', '   '],
    ['source_url', '   '],
    ['source_url', 'https://'],
    ['retrieved_at', ''],
    ['source_name', undefined],
  ])('renders nothing when %s is %j', (field, value) => {
    const { container } = renderItem(patch(sameDayPhoto, { [field]: value }))
    expect(container).toBeEmptyDOMElement()
  })

  it.each([
    ['url', 'javascript:alert(1)'],
    ['url', '/commons/Example.jpg'],
    ['embed_url', 'javascript:alert(1)'],
    ['thumbnail_url', '//media.example.org/thumb.jpg'],
  ])('renders nothing when %s is %j (M3a3 R9)', (field, value) => {
    const { container } = renderItem(patch(sameDayPhoto, { [field]: value }))
    expect(container).toBeEmptyDOMElement()
  })
})
