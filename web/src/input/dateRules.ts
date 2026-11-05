// Date rules for the input form (plans M2b and C1 S2 "웹 상수").
// The same two values as the server's StoryDate.TODAY_ZONE and START_DATE
// (server/src/main/java/io/github/ericmschoi/theworldthatday/story/
// StoryDate.java): change both together.
// "Today" is the calendar day in Canada's earliest zone (decision 2 (a)).
export const TODAY_ZONE = 'America/St_Johns'
export const START_DATE = '1950-01-01'
export const START_YEAR = 1950

export type DateCode =
  'DATE_REQUIRED' | 'DATE_NOT_EXIST' | 'DATE_IN_FUTURE' | 'DATE_TOO_EARLY'

export const DATE_MESSAGES: Record<DateCode, string> = {
  DATE_REQUIRED: 'Choose your date of birth.',
  DATE_NOT_EXIST: "That date doesn't exist. Please check it.",
  DATE_IN_FUTURE: "Dates after today can't be shown.",
  DATE_TOO_EARLY: 'The earliest date you can choose is January 1, 1950.',
}

// The three select boxes' values; '' while not chosen.
export type DateParts = { year: string; month: string; day: string }

export type DateCheck = { date: string } | { code: DateCode }

const pad = (n: number) => String(n).padStart(2, '0')

// Today's date (YYYY-MM-DD) in TODAY_ZONE, as the server judges "today".
export function serviceToday(now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: TODAY_ZONE,
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  }).formatToParts(now)
  const part = (type: string) =>
    Number(parts.find((p) => p.type === type)?.value)
  return `${part('year')}-${pad(part('month'))}-${pad(part('day'))}`
}

// Years for the year box: this year (TODAY_ZONE) down to the start year.
export function yearOptions(today: string = serviceToday()): number[] {
  const years: number[] = []
  for (let y = Number(today.slice(0, 4)); y >= START_YEAR; y--) years.push(y)
  return years
}

// Checks in the server's order: required, exists, future, too early.
export function checkDate(
  { year, month, day }: DateParts,
  today: string = serviceToday(),
): DateCheck {
  if (year === '' || month === '' || day === '') {
    return { code: 'DATE_REQUIRED' }
  }
  const [y, m, d] = [year, month, day].map(Number)
  const at = new Date(Date.UTC(y, m - 1, d))
  if (
    !Number.isInteger(y + m + d) ||
    at.getUTCFullYear() !== y ||
    at.getUTCMonth() !== m - 1 ||
    at.getUTCDate() !== d
  ) {
    return { code: 'DATE_NOT_EXIST' }
  }
  const date = `${String(y).padStart(4, '0')}-${pad(m)}-${pad(d)}`
  if (date > today) return { code: 'DATE_IN_FUTURE' }
  if (date < START_DATE) return { code: 'DATE_TOO_EARLY' }
  return { date }
}

// en-CA names, written out so the words do not depend on the ICU version.
export const MONTH_NAMES = (
  'January February March April May June July August September October ' +
  'November December'
).split(' ')
const WEEKDAYS =
  'Sunday Monday Tuesday Wednesday Thursday Friday Saturday'.split(' ')

// The weekday of a YYYY-MM-DD date, from the date alone (no time zone).
export function weekday(date: string): string {
  const [y, m, d] = date.split('-').map(Number)
  return WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]
}

// "3:20 p.m." from "15:20", en-CA style; zero minutes are kept ("9:00 a.m.").
export function timeWording(time: string): string {
  const [h, min] = time.split(':').map(Number)
  const hour = h % 12 === 0 ? 12 : h % 12
  return `${hour}:${pad(min)} ${h < 12 ? 'a.m.' : 'p.m.'}`
}

// The opening's date part: "Saturday, February 13, 1988", plus
// " at 3:20 p.m." if a time is given (decision S2-P2 (a)).
export function openingDate(date: string, time?: string): string {
  const [y, m, d] = date.split('-').map(Number)
  const day = `${weekday(date)}, ${MONTH_NAMES[m - 1]} ${d}, ${y}`
  return time ? `${day} at ${timeWording(time)}` : day
}
