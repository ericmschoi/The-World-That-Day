// Plan M2b T2 (1)·(2): the date rules. Dates are the demo dates (product brief
// 12), boundary values and the start date only.
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  checkDate,
  openingDate,
  seoulToday,
  timeWording,
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
    ['1988-09-17', '토요일'],
    ['2002-06-22', '토요일'],
    ['1995-03-15', '수요일'],
    ['2000-02-29', '화요일'],
    ['1950-01-01', '일요일'],
  ])('%s is %s', (date, day) => {
    expect(weekday(date)).toBe(day)
  })
})

describe('checkDate', () => {
  it.each(['1988-09-17', '2002-06-22', '1995-03-15', '2000-02-29'])(
    '%s passes',
    (date) => {
      expect(checkDate(parts(date), TODAY)).toEqual({ date })
    },
  )

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

  it.each(['2001-02-29', '1988-02-30', '1995-04-31'])(
    'refuses %s as DATE_NOT_EXIST',
    (date) => {
      expect(checkDate(parts(date), TODAY)).toEqual({ code: 'DATE_NOT_EXIST' })
    },
  )

  it.each([
    { year: '', month: '9', day: '17' },
    { year: '1988', month: '', day: '17' },
    { year: '1988', month: '9', day: '' },
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

describe('today in Asia/Seoul (fake clock)', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('at UTC 14:59 Seoul is still 2026-10-27: today passes, tomorrow not', () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-27T14:59:59Z'))
    expect(seoulToday()).toBe('2026-10-27')
    expect(checkDate(parts('2026-10-27'))).toEqual({ date: '2026-10-27' })
    expect(checkDate(parts('2026-10-28'))).toEqual({ code: 'DATE_IN_FUTURE' })
  })

  it('at UTC 15:00 Seoul is 2026-10-28: that day passes, the next not', () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-27T15:00:00Z'))
    expect(seoulToday()).toBe('2026-10-28')
    expect(checkDate(parts('2026-10-28'))).toEqual({ date: '2026-10-28' })
    expect(checkDate(parts('2026-10-29'))).toEqual({ code: 'DATE_IN_FUTURE' })
  })

  it('starts the year list with the Seoul year on New Year', () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-12-31T15:00:00Z'))
    expect(yearOptions()[0]).toBe(2027)
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
    ['15:20', '오후 3시 20분'],
    ['00:05', '오전 0시 5분'],
    ['12:00', '오후 12시'],
    ['09:00', '오전 9시'],
  ])('%s reads %s', (time, words) => {
    expect(timeWording(time)).toBe(words)
  })

  it.each([
    ['1988-09-17', undefined, '1988년 9월 17일 토요일'],
    ['2002-06-22', '15:20', '2002년 6월 22일 토요일 오후 3시 20분'],
    ['1995-03-15', '00:05', '1995년 3월 15일 수요일 오전 0시 5분'],
  ])('%s with time %s reads %s', (date, time, words) => {
    expect(openingDate(date, time)).toBe(words)
  })
})
