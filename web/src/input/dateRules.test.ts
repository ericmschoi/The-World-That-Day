// Plans M2b T2 (1)·(2) and C1 S2 T8a (2): the date rules. Dates are the
// demo dates (Canada research decision 1 (b)), boundary values and the start
// date only.
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  checkDate,
  openingDate,
  serviceToday,
  START_DATE,
  timeWording,
  TODAY_ZONE,
  weekday,
  yearOptions,
  type DateParts,
} from './dateRules'

const TODAY = '2026-10-27'

const parts = (date: string): DateParts => {
  const [year, month, day] = date.split('-').map((n) => String(Number(n)))
  return { year, month, day }
}

describe('weekday', () => {
  it.each([
    ['1988-02-13', 'Saturday'],
    ['1976-07-17', 'Saturday'],
    ['2010-02-12', 'Friday'],
    ['1980-04-12', 'Saturday'],
    ['2000-02-29', 'Tuesday'],
    ['1950-01-01', 'Sunday'],
  ])('%s is %s', (date, day) => {
    expect(weekday(date)).toBe(day)
  })
})

describe('checkDate', () => {
  it.each([
    '1988-02-13',
    '1976-07-17',
    '2010-02-12',
    '1980-04-12',
    '2000-02-29',
  ])('%s passes', (date) => {
    expect(checkDate(parts(date), TODAY)).toEqual({ date })
  })

  it('passes the start date 1950-01-01', () => {
    expect(checkDate(parts('1950-01-01'), TODAY)).toEqual({
      date: '1950-01-01',
    })
  })

  it('refuses 1949-12-31 as DATE_TOO_EARLY', () => {
    expect(checkDate(parts('1949-12-31'), TODAY)).toEqual({
      code: 'DATE_TOO_EARLY',
    })
  })

  it.each(['2001-02-29', '1988-02-30', '1980-04-31'])(
    'refuses %s as DATE_NOT_EXIST',
    (date) => {
      expect(checkDate(parts(date), TODAY)).toEqual({ code: 'DATE_NOT_EXIST' })
    },
  )

  it.each([
    { year: '', month: '2', day: '13' },
    { year: '1988', month: '', day: '13' },
    { year: '1988', month: '2', day: '' },
    { year: '', month: '', day: '' },
  ])('refuses a missing part as DATE_REQUIRED (%o)', (value) => {
    expect(checkDate(value, TODAY)).toEqual({ code: 'DATE_REQUIRED' })
  })

  it('passes today and refuses tomorrow as DATE_IN_FUTURE', () => {
    expect(checkDate(parts(TODAY), TODAY)).toEqual({ date: TODAY })
    expect(checkDate(parts('2026-10-28'), TODAY)).toEqual({
      code: 'DATE_IN_FUTURE',
    })
  })
})

// The same instants as the server's StoryDate tests (C1 S1 T5). St. John's
// is UTC-3:30 in standard time and UTC-2:30 in daylight time.
describe("today in St. John's (fake clock)", () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('uses the same zone and start date as the server StoryDate', () => {
    expect(TODAY_ZONE).toBe('America/St_Johns')
    expect(START_DATE).toBe('1950-01-01')
  })

  it.each([
    ['standard time', '2026-01-15T03:29:59Z', '2026-01-14'],
    ['daylight time', '2026-07-15T02:29:59Z', '2026-07-14'],
  ])('in %s, at %s, the 15th is still in the future', (_, instant, today) => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(instant))
    const next = instant.slice(0, 10)
    expect(serviceToday()).toBe(today)
    expect(checkDate(parts(today))).toEqual({ date: today })
    expect(checkDate(parts(next))).toEqual({ code: 'DATE_IN_FUTURE' })
  })

  it.each([
    ['standard time', '2026-01-15T03:30:00Z'],
    ['daylight time', '2026-07-15T02:30:00Z'],
  ])('in %s, at %s, the 15th passes', (_, instant) => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(instant))
    const day = instant.slice(0, 10)
    expect(serviceToday()).toBe(day)
    expect(checkDate(parts(day))).toEqual({ date: day })
  })

  it.each([
    ['2027-01-01T03:29:59Z', 2026],
    ['2027-01-01T03:30:00Z', 2027],
  ])("at %s the year list starts with the St. John's year %i", (at, year) => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(at))
    expect(yearOptions()[0]).toBe(year)
  })
})

describe('yearOptions', () => {
  it('lists every year from this year down to 1950', () => {
    const years = yearOptions(TODAY)
    expect(years).toHaveLength(2026 - 1950 + 1)
    expect(years[0]).toBe(2026)
    expect(years.at(-1)).toBe(1950)
    years.forEach((year, i) => expect(year).toBe(2026 - i))
  })
})

describe('opening wording', () => {
  it.each([
    ['15:20', '3:20 p.m.'],
    ['00:05', '12:05 a.m.'],
    ['12:00', '12:00 p.m.'],
    ['09:00', '9:00 a.m.'],
  ])('%s reads %s', (time, words) => {
    expect(timeWording(time)).toBe(words)
  })

  it.each([
    ['1988-02-13', undefined, 'Saturday, February 13, 1988'],
    ['1976-07-17', '15:20', 'Saturday, July 17, 1976 at 3:20 p.m.'],
    ['2010-02-12', '00:05', 'Friday, February 12, 2010 at 12:05 a.m.'],
  ])('%s with time %s reads %s', (date, time, words) => {
    expect(openingDate(date, time)).toBe(words)
  })

  // dateRules.ts writes the en-CA words out by hand. ICU builds differ in
  // the space characters they use, so only the spaces are made plain.
  it('matches Intl en-CA for the date and time, spaces made plain', () => {
    const intl = new Intl.DateTimeFormat('en-CA', {
      dateStyle: 'full',
      timeStyle: 'short',
      timeZone: 'UTC',
    })
      .format(new Date(Date.UTC(1976, 6, 17, 15, 20)))
      .replace(/\s/g, ' ')
    expect(openingDate('1976-07-17', '15:20')).toBe(intl)
  })
})
