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
const THAT_DAY_HINTS = ['같은 날', '같은 달', '같은 해', '그날', '무렵']

describe('honest date boundaries on the card (demo 1988-09-17)', () => {
  it.each([
    // [case, date_relation, date_precision, captured_at, badge, first line]
    [
      'same day',
      'same_day',
      0,
      'day',
      '1988-09-17',
      '같은 날',
      '같은 날 1988년 9월 17일 촬영',
    ],
    [
      'same month, other day',
      'same_month',
      0,
      'day',
      '1988-09-02',
      '같은 달',
      '같은 달 1988년 9월 2일 촬영',
    ],
    [
      'same month, month precision',
      'same_month',
      0,
      'month',
      '1988-09',
      '같은 달',
      '같은 달 1988년 9월 촬영',
    ],
    [
      'same year, other month',
      'same_year',
      0,
      'month',
      '1988-03',
      '같은 해',
      '같은 해 1988년 3월 촬영',
    ],
    [
      'same year, year precision',
      'same_year',
      0,
      'year',
      '1988',
      '같은 해',
      '같은 해 1988년 촬영',
    ],
    [
      'same year, leap day',
      'same_year',
      0,
      'day',
      '1988-02-29',
      '같은 해',
      '같은 해 1988년 2월 29일 촬영',
    ],
    [
      'different year, before',
      'years_apart',
      -1,
      'year',
      '1987',
      '1년 전',
      '1년 전 1987년 촬영',
    ],
    [
      'different year, after',
      'years_apart',
      2,
      'day',
      '1990-09-17',
      '2년 뒤',
      '2년 뒤 1990년 9월 17일 촬영',
    ],
    [
      'approximate, same year',
      'approximate',
      0,
      'approx',
      '1988',
      '같은 해 무렵',
      '같은 해 무렵 1988년 무렵 촬영(추정)',
    ],
    [
      'approximate, after',
      'approximate',
      3,
      'approx',
      '1991',
      '3년 뒤 무렵',
      '3년 뒤 무렵 1991년 무렵 촬영(추정)',
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
    '%s: shown, labeled only 촬영 시점 미상',
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
      expect(firstLine(figure)).toHaveTextContent(/^촬영 시점 미상$/)
      expect(firstLine(figure).querySelector('span')).toBeNull()
      expect(figure.textContent).not.toContain('1988년 9월 17일')
      for (const hint of THAT_DAY_HINTS) {
        expect(figure.textContent).not.toContain(hint)
      }
    },
  )
})

describe('unknown capture time is never presented as that day', () => {
  // Undated variants of a photo for each demo input with media.
  it.each([
    ['1988-09-17', undatedPhoto, '1988년 9월 17일'],
    [
      '2002-06-22',
      patch(koglPhoto, {
        captured_at: null,
        date_precision: null,
        date_relation: { kind: 'unknown', years: 0 },
      }),
      '2002년 6월 22일',
    ],
  ])('demo %s: image shown, no badge, no requested date', (_d, media, day) => {
    renderItem(media)
    const figure = screen.getByRole('figure')
    expect(within(figure).getByRole('img')).toHaveAttribute(
      'src',
      media.thumbnail_url,
    )
    expect(firstLine(figure)).toHaveTextContent(/^촬영 시점 미상$/)
    expect(
      figure.querySelector('figcaption span:not([aria-hidden])'),
    ).toBeNull()
    expect(figure.textContent).not.toContain(day)
    for (const hint of THAT_DAY_HINTS) {
      expect(figure.textContent).not.toContain(hint)
    }
  })

  it('ignores a leftover captured_at when the precision is unknown', () => {
    renderItem(patch(undatedPhoto, { captured_at: '1988-09-17' }))
    const figure = screen.getByRole('figure')
    expect(firstLine(figure)).toHaveTextContent(/^촬영 시점 미상$/)
    expect(figure.textContent).not.toContain('1988년 9월 17일')
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
      expect(alt).not.toContain('1988년 9월 17일')
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
      // The only links are the caption's 원문 보기 and 라이선스.
      for (const link of within(figure).getAllByRole('link')) {
        expect(link.closest('figcaption')).not.toBeNull()
        expect(['원문 보기', '라이선스']).toContain(link.textContent)
      }
    },
  )
})

describe('source, license and capture time are always on the card', () => {
  it.each([sameDayPhoto, undatedPhoto, koglPhoto, unlicensedPhoto])(
    '$id: caption shows the source text and the 원문 보기 link',
    (media) => {
      renderItem(media)
      const figure = screen.getByRole('figure')
      expect(figure).toHaveTextContent(media.attribution)
      expect(
        within(figure).getByRole('link', { name: '원문 보기' }),
      ).toHaveAttribute('href', media.source_url)
      expect(firstLine(figure).textContent).not.toBe('')
    },
  )

  it.each([
    ['license_status unknown with a name', { license_status: 'unknown' }],
    ['license null', { license: null }],
    ['license blank', { license: '   ' }],
  ])('%s: 라이선스 확인 안 됨, image still shown', (_case, fields) => {
    renderItem(patch(sameDayPhoto, fields))
    const figure = screen.getByRole('figure')
    expect(within(figure).getByRole('img')).toBeInTheDocument()
    expect(within(figure).getByText('라이선스 확인 안 됨')).toBeInTheDocument()
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
