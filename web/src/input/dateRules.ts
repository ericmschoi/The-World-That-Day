// Date rules for the input form (plan M2b "웹: 화면과 상태", decision #3 (a)).
// The start date also lives in the server's StoryDate: change both together.
export const START_DATE = '1950-01-01'
export const START_YEAR = 1950

export type DateCode =
  'DATE_REQUIRED' | 'DATE_NOT_EXIST' | 'DATE_IN_FUTURE' | 'DATE_TOO_EARLY'

export const DATE_MESSAGES: Record<DateCode, string> = {
  DATE_REQUIRED: '생년월일을 골라 주세요.',
  DATE_NOT_EXIST: '없는 날짜입니다. 날짜를 다시 확인해 주세요.',
  DATE_IN_FUTURE: '오늘 이후의 날짜는 볼 수 없습니다.',
  DATE_TOO_EARLY: '1950년 1월 1일부터 볼 수 있습니다.',
}

// The three select boxes' values; '' while not chosen.
export type DateParts = { year: string; month: string; day: string }

export type DateCheck = { date: string } | { code: DateCode }

const pad = (n: number) => String(n).padStart(2, '0')

// Today's date (YYYY-MM-DD) in Asia/Seoul, as the server judges "today".
export function seoulToday(now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  }).formatToParts(now)
  const part = (type: string) =>
    Number(parts.find((p) => p.type === type)?.value)
  return `${part('year')}-${pad(part('month'))}-${pad(part('day'))}`
}

// Years for the year box: this year (Asia/Seoul) down to the start year.
export function yearOptions(today: string = seoulToday()): number[] {
  const years: number[] = []
  for (let y = Number(today.slice(0, 4)); y >= START_YEAR; y--) years.push(y)
  return years
}

// Checks in the server's order: required, exists, future, too early.
export function checkDate(
  { year, month, day }: DateParts,
  today: string = seoulToday(),
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

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

// The weekday of a YYYY-MM-DD date, from the date alone (no time zone).
export function weekday(date: string): string {
  const [y, m, d] = date.split('-').map(Number)
  return `${WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]}요일`
}

// "오후 3시 20분" from "15:20"; the minutes are left out when zero.
export function timeWording(time: string): string {
  const [h, min] = time.split(':').map(Number)
  const hour = h === 12 ? 12 : h % 12
  return `${h < 12 ? '오전' : '오후'} ${hour}시${min === 0 ? '' : ` ${min}분`}`
}

// The opening's date part: "1988년 9월 17일 토요일", plus the time if given.
export function openingDate(date: string, time?: string): string {
  const [y, m, d] = date.split('-').map(Number)
  const day = `${y}년 ${m}월 ${d}일 ${weekday(date)}`
  return time ? `${day} ${timeWording(time)}` : day
}
