import type { DatePrecision, DateRelation, Media } from './types'

export type DateLabel = { badge: string | null; text: string }

// The badge `DayMediaGroups` reads to put a card in its same-day group.
// Exported so the grouping never spells the text out again.
export const SAME_DAY = 'Same day'

const UNKNOWN: DateLabel = { badge: null, text: 'Capture time unknown' }

// A table, not `Intl`: ICU builds differ in the spaces they put in dates.
// The result matches the en-CA long date ("September 17, 1988").
const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
]

// `captured_at` shape for each precision (M3a3 rule R8).
const SHAPES: Partial<Record<DatePrecision, RegExp>> = {
  day: /^(\d{4})-(\d{2})-(\d{2})$/,
  month: /^(\d{4})-(\d{2})$/,
  year: /^(\d{4})$/,
  approx: /^(\d{4})$/,
}

// Precisions each relation needs, so a badge never claims more than the
// capture time supports (product brief 6.1).
const NEEDS: Partial<Record<DateRelation['kind'], DatePrecision[]>> = {
  same_day: ['day'],
  same_month: ['day', 'month'],
  same_year: ['day', 'month', 'year'],
  years_apart: ['day', 'month', 'year'],
  approximate: ['approx'],
}

const isLeapYear = (year: number) =>
  year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)

const DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

const daysInMonth = (year: number, month: number) =>
  month === 2 && isLeapYear(year) ? 29 : DAYS_IN_MONTH[month - 1]

// Rejects calendar values that do not exist (1988-13, 1988-09-32, 2001-02-29).
function isRealDate(year: string, month?: string, day?: string): boolean {
  if (month === undefined) return true
  const m = Number(month)
  if (m < 1 || m > 12) return false
  if (day === undefined) return true
  const d = Number(day)
  return d >= 1 && d <= daysInMonth(Number(year), m)
}

function formatCapturedAt(media: Media): string | null {
  const shape = media.date_precision && SHAPES[media.date_precision]
  const parts = shape?.exec(media.captured_at ?? '')
  if (!parts) return null
  const [, year, month, day] = parts
  if (!isRealDate(year, month, day)) return null
  if (month === undefined) return year
  const name = MONTHS[Number(month) - 1]
  return day === undefined
    ? `${name} ${year}`
    : `${name} ${Number(day)}, ${year}`
}

// Distance from the requested year ("3 years earlier" / "1 year later").
function distance(years: number): string {
  const count = Math.abs(years)
  const unit = count === 1 ? 'year' : 'years'
  return `${count} ${unit} ${years < 0 ? 'earlier' : 'later'}`
}

function badgeFor({ kind, years }: DateRelation): string | null {
  switch (kind) {
    case 'same_day':
      return SAME_DAY
    case 'same_month':
      return 'Same month'
    case 'same_year':
      return 'Same year'
    case 'approximate':
      // The year is an estimate, so the badge keeps "Around" / "About"
      // (product brief 6.1).
      if (!Number.isInteger(years)) return null
      return years === 0 ? 'Around the same year' : `About ${distance(years)}`
    case 'years_apart':
      if (!Number.isInteger(years)) return null
      return years === 0 ? null : distance(years)
    default:
      return null
  }
}

// Badge and capture-time text for the caption. Anything inconsistent falls
// back to "Capture time unknown" rather than a badge that may be wrong.
export function dateLabel(media: Media): DateLabel {
  const relation = media.date_relation
  const precision = media.date_precision
  if (relation?.kind === 'current' && precision === 'current') {
    return { badge: 'Present day', text: 'Capture time not provided' }
  }
  const allowed =
    relation && precision && NEEDS[relation.kind]?.includes(precision)
  const badge = allowed ? badgeFor(relation) : null
  const when = formatCapturedAt(media)
  if (!badge || !when) return UNKNOWN
  const text =
    precision === 'approx'
      ? `Taken around ${when} (estimated)`
      : `Taken ${when}`
  return { badge, text }
}
