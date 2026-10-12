import { describe, expect, it } from 'vitest'
import { sameDayPhoto, undatedPhoto } from '../mocks/fixtures/media/photos'
import { sdkRoadview } from '../mocks/fixtures/media/modes'
import { dateLabel } from './dateLabel'
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
      '같은 날',
      '1988년 9월 17일 촬영',
    ],
    [
      'same_month at day precision (D = 2000-02-29)',
      photo('2000-02-01', 'day', { kind: 'same_month', years: 0 }),
      '같은 달',
      '2000년 2월 1일 촬영',
    ],
    [
      'same_month at month precision',
      photo('1988-09', 'month', { kind: 'same_month', years: 0 }),
      '같은 달',
      '1988년 9월 촬영',
    ],
    [
      'same_year at year precision',
      photo('1988', 'year', { kind: 'same_year', years: 0 }),
      '같은 해',
      '1988년 촬영',
    ],
    [
      'same_year at month precision',
      photo('1988-03', 'month', { kind: 'same_year', years: 0 }),
      '같은 해',
      '1988년 3월 촬영',
    ],
    [
      'years_apart, earlier',
      photo('1987', 'year', { kind: 'years_apart', years: -1 }),
      '1년 전',
      '1987년 촬영',
    ],
    [
      'years_apart across the leap day (D = 2000-02-29)',
      photo('1999-02-28', 'day', { kind: 'years_apart', years: -1 }),
      '1년 전',
      '1999년 2월 28일 촬영',
    ],
    [
      'years_apart, later',
      photo('1990', 'year', { kind: 'years_apart', years: 2 }),
      '2년 뒤',
      '1990년 촬영',
    ],
    [
      'approximate, same year',
      photo('1988', 'approx', { kind: 'approximate', years: 0 }),
      '같은 해 무렵',
      '1988년 무렵 촬영(추정)',
    ],
    [
      'approximate, earlier',
      photo('1985', 'approx', { kind: 'approximate', years: -3 }),
      '3년 전 무렵',
      '1985년 무렵 촬영(추정)',
    ],
    [
      'approximate, later',
      photo('1990', 'approx', { kind: 'approximate', years: 2 }),
      '2년 뒤 무렵',
      '1990년 무렵 촬영(추정)',
    ],
    [
      'leap day exists in a leap year (D = 2000-02-29)',
      photo('2000-02-29', 'day', { kind: 'same_day', years: 0 }),
      '같은 날',
      '2000년 2월 29일 촬영',
    ],
    [
      'last day of a 31-day month (D = 1988-09-17)',
      photo('1988-08-31', 'day', { kind: 'same_year', years: 0 }),
      '같은 해',
      '1988년 8월 31일 촬영',
    ],
    [
      'month 12 at month precision (D = 2002-06-22)',
      photo('2001-12', 'month', { kind: 'years_apart', years: -1 }),
      '1년 전',
      '2001년 12월 촬영',
    ],
  ])('%s', (_name, media, badge, text) => {
    expect(dateLabel(media)).toEqual({ badge, text })
  })

  it('current → 현재 모습, capture time not provided', () => {
    expect(dateLabel(sdkRoadview)).toEqual({
      badge: '현재 모습',
      text: '촬영 시점 미제공',
    })
  })

  it('unknown → no badge, 촬영 시점 미상', () => {
    expect(dateLabel(undatedPhoto)).toEqual({
      badge: null,
      text: '촬영 시점 미상',
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
  ])('falls back to 촬영 시점 미상 when inconsistent: %s', (_name, media) => {
    expect(dateLabel(media)).toEqual({ badge: null, text: '촬영 시점 미상' })
  })

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
      'February 30 in a leap year (2000-02-30)',
      photo('2000-02-30', 'day', { kind: 'same_month', years: 0 }),
    ],
  ])(
    'falls back to 촬영 시점 미상 for an impossible date: %s',
    (_name, media) => {
      expect(dateLabel(media)).toEqual({ badge: null, text: '촬영 시점 미상' })
    },
  )
})
