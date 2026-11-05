import { describe, expect, it } from 'vitest'
import { sameDayPhoto, undatedPhoto } from '../mocks/fixtures/media/photos'
import { sdkRoadview } from '../mocks/fixtures/media/modes'
import { dateLabel, SAME_DAY } from './dateLabel'
import type { Media } from './types'

// Requested dates are the demo input 1988-09-17 and the leap-day boundary
// 2000-02-29 (CLAUDE.md 7.6). The server computes `date_relation`.
const photo = (
  captured_at: string | null,
  date_precision: Media['date_precision'],
  date_relation: Media['date_relation'],
): Media => ({ ...sameDayPhoto, captured_at, date_precision, date_relation })

describe('dateLabel', () => {
  it.each([
    [
      'same_day',
      photo('1988-09-17', 'day', { kind: 'same_day', years: 0 }),
      'Same day',
      'Taken September 17, 1988',
    ],
    [
      'same_month at day precision (D = 2000-02-29)',
      photo('2000-02-01', 'day', { kind: 'same_month', years: 0 }),
      'Same month',
      'Taken February 1, 2000',
    ],
    [
      'same_month at month precision',
      photo('1988-09', 'month', { kind: 'same_month', years: 0 }),
      'Same month',
      'Taken September 1988',
    ],
    [
      'same_year at year precision',
      photo('1988', 'year', { kind: 'same_year', years: 0 }),
      'Same year',
      'Taken 1988',
    ],
    [
      'same_year at month precision',
      photo('1988-03', 'month', { kind: 'same_year', years: 0 }),
      'Same year',
      'Taken March 1988',
    ],
    [
      'years_apart, earlier',
      photo('1987', 'year', { kind: 'years_apart', years: -1 }),
      '1 year earlier',
      'Taken 1987',
    ],
    [
      'years_apart across the leap day (D = 2000-02-29)',
      photo('1999-02-28', 'day', { kind: 'years_apart', years: -1 }),
      '1 year earlier',
      'Taken February 28, 1999',
    ],
    [
      'years_apart, one year later',
      photo('1989', 'year', { kind: 'years_apart', years: 1 }),
      '1 year later',
      'Taken 1989',
    ],
    [
      'years_apart, three years later',
      photo('1991-09', 'month', { kind: 'years_apart', years: 3 }),
      '3 years later',
      'Taken September 1991',
    ],
    [
      'years_apart, three years earlier',
      photo('1985-09-17', 'day', { kind: 'years_apart', years: -3 }),
      '3 years earlier',
      'Taken September 17, 1985',
    ],
    [
      'years_apart, later',
      photo('1990', 'year', { kind: 'years_apart', years: 2 }),
      '2 years later',
      'Taken 1990',
    ],
    [
      'approximate, same year',
      photo('1988', 'approx', { kind: 'approximate', years: 0 }),
      'Around the same year',
      'Taken around 1988 (estimated)',
    ],
    [
      'approximate, earlier',
      photo('1985', 'approx', { kind: 'approximate', years: -3 }),
      'About 3 years earlier',
      'Taken around 1985 (estimated)',
    ],
    [
      'approximate, one year earlier',
      photo('1987', 'approx', { kind: 'approximate', years: -1 }),
      'About 1 year earlier',
      'Taken around 1987 (estimated)',
    ],
    [
      'approximate, one year later',
      photo('1989', 'approx', { kind: 'approximate', years: 1 }),
      'About 1 year later',
      'Taken around 1989 (estimated)',
    ],
    [
      'approximate, later',
      photo('1990', 'approx', { kind: 'approximate', years: 2 }),
      'About 2 years later',
      'Taken around 1990 (estimated)',
    ],
    [
      'leap day exists in a leap year (D = 2000-02-29)',
      photo('2000-02-29', 'day', { kind: 'same_day', years: 0 }),
      'Same day',
      'Taken February 29, 2000',
    ],
    [
      'last day of a 31-day month (D = 1988-09-17)',
      photo('1988-08-31', 'day', { kind: 'same_year', years: 0 }),
      'Same year',
      'Taken August 31, 1988',
    ],
    [
      'month 12 at month precision (D = 2002-06-22)',
      photo('2001-12', 'month', { kind: 'years_apart', years: -1 }),
      '1 year earlier',
      'Taken December 2001',
    ],
  ])('%s', (_name, media, badge, text) => {
    expect(dateLabel(media)).toEqual({ badge, text })
  })

  it('exports the same-day badge text for the day media groups', () => {
    expect(SAME_DAY).toBe('Same day')
    expect(
      dateLabel(photo('1988-09-17', 'day', { kind: 'same_day', years: 0 }))
        .badge,
    ).toBe(SAME_DAY)
  })

  it('current → 현재 모습, capture time not provided', () => {
    expect(dateLabel(sdkRoadview)).toEqual({
      badge: 'Present day',
      text: 'Capture time not provided',
    })
  })

  it('unknown → no badge, 촬영 시점 미상', () => {
    expect(dateLabel(undatedPhoto)).toEqual({
      badge: null,
      text: 'Capture time unknown',
    })
  })

  // A badge must never claim more than the capture time supports (6.1).
  it.each([
    [
      'same_day at month precision',
      photo('1988-09', 'month', { kind: 'same_day', years: 0 }),
    ],
    [
      'same_month at year precision',
      photo('1988', 'year', { kind: 'same_month', years: 0 }),
    ],
    [
      'approximate at day precision',
      photo('1988-09-17', 'day', { kind: 'approximate', years: 0 }),
    ],
    [
      'current without current precision',
      photo('1988-09-17', 'day', { kind: 'current', years: 0 }),
    ],
    [
      'years_apart of zero',
      photo('1988', 'year', { kind: 'years_apart', years: 0 }),
    ],
    [
      'captured_at not matching precision',
      photo('1988-09', 'day', { kind: 'same_day', years: 0 }),
    ],
    [
      'captured_at with a one-digit month',
      photo('1988-9-17', 'day', { kind: 'same_day', years: 0 }),
    ],
    [
      'captured_at with a time of day',
      photo('1988-09-17T00:00:00Z', 'day', { kind: 'same_day', years: 0 }),
    ],
    [
      'captured_at with a two-digit year',
      photo('88', 'year', { kind: 'same_year', years: 0 }),
    ],
    ['captured_at missing', photo(null, 'day', { kind: 'same_day', years: 0 })],
    [
      'date_precision null (unknown) with a same_day relation',
      photo('1988-09-17', null, { kind: 'same_day', years: 0 }),
    ],
    [
      'date_precision null (unknown) with a same_year relation',
      photo('1988', null, { kind: 'same_year', years: 0 }),
    ],
    [
      'approximate with a non-integer distance',
      photo('1988', 'approx', { kind: 'approximate', years: 0.5 }),
    ],
    [
      'approximate with a missing distance',
      photo('1988', 'approx', {
        kind: 'approximate',
        years: undefined,
      } as unknown as Media['date_relation']),
    ],
    [
      'years_apart with a non-integer distance',
      photo('1987', 'year', { kind: 'years_apart', years: Number.NaN }),
    ],
    [
      'date_relation missing',
      { ...sameDayPhoto, date_relation: undefined } as unknown as Media,
    ],
  ])(
    'falls back to Capture time unknown when inconsistent: %s',
    (_name, media) => {
      expect(dateLabel(media)).toEqual({
        badge: null,
        text: 'Capture time unknown',
      })
    },
  )

  // Calendar values that do not exist must not reach the caption.
  it.each([
    ['month 13', photo('1988-13', 'month', { kind: 'same_year', years: 0 })],
    ['month 00', photo('1988-00', 'month', { kind: 'same_year', years: 0 })],
    [
      'month 13 at day precision',
      photo('1988-13-17', 'day', { kind: 'same_year', years: 0 }),
    ],
    [
      'September 32',
      photo('1988-09-32', 'day', { kind: 'same_month', years: 0 }),
    ],
    [
      'September 31 (30-day month)',
      photo('1988-09-31', 'day', { kind: 'same_month', years: 0 }),
    ],
    ['day 00', photo('1988-09-00', 'day', { kind: 'same_month', years: 0 })],
    [
      'February 29 in a common year (2001-02-29)',
      photo('2001-02-29', 'day', { kind: 'years_apart', years: 1 }),
    ],
    [
      'February 30 (1988-02-30)',
      photo('1988-02-30', 'day', { kind: 'same_year', years: 0 }),
    ],
    [
      'February 30 in a leap year (2000-02-30)',
      photo('2000-02-30', 'day', { kind: 'same_month', years: 0 }),
    ],
  ])(
    'falls back to Capture time unknown for an impossible date: %s',
    (_name, media) => {
      expect(dateLabel(media)).toEqual({
        badge: null,
        text: 'Capture time unknown',
      })
    },
  )
})
