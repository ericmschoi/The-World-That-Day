// Plans M2b T2 (3) and C1 S2 T8a (3): the birth-date select boxes. Dates
// are demo dates and boundary values only.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BirthDateField } from './BirthDateField'
import { DATE_MESSAGES, type DateCode, type DateParts } from './dateRules'

const EMPTY: DateParts = { year: '', month: '', day: '' }

// Option values, without the leading "Select" placeholder.
const values = (name: string) =>
  within(screen.getByRole('combobox', { name }))
    .getAllByRole('option')
    .map((option) => (option as HTMLOptionElement).value)
    .filter((value) => value !== '')

function Field({ error }: { error?: DateCode }) {
  const [value, setValue] = useState(EMPTY)
  return (
    <>
      <BirthDateField value={value} onChange={setValue} error={error} />
      <output>{`${value.year}-${value.month}-${value.day}`}</output>
    </>
  )
}

afterEach(() => {
  vi.useRealTimers()
})

describe('BirthDateField', () => {
  it('is a group named Date of birth (required) with boxes Year, Month, Day', () => {
    render(<Field />)
    const group = screen.getByRole('group', {
      name: 'Date of birth (required)',
    })
    const boxes = within(group).getAllByRole('combobox')
    expect(boxes).toHaveLength(3)
    expect(boxes[0]).toHaveAccessibleName('Year')
    expect(boxes[1]).toHaveAccessibleName('Month')
    expect(boxes[2]).toHaveAccessibleName('Day')
    for (const box of boxes) {
      expect(box).toHaveValue('')
      expect(box).not.toHaveAttribute('aria-invalid')
      expect(box).not.toHaveAccessibleDescription()
    }
  })

  it("lists every year from this year (St. John's) down to 1950", () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-27T03:00:00Z'))
    render(<Field />)
    const expected = Array.from({ length: 2026 - 1950 + 1 }, (_, i) =>
      String(2026 - i),
    )
    expect(values('Year')).toEqual(expected)
  })

  it('lists 12 months and 31 days', () => {
    render(<Field />)
    expect(values('Month')).toEqual(
      Array.from({ length: 12 }, (_, i) => String(i + 1)),
    )
    expect(values('Day')).toEqual(
      Array.from({ length: 31 }, (_, i) => String(i + 1)),
    )
  })

  it('names the months in English, keeping the values 1 to 12', () => {
    render(<Field />)
    const months = within(screen.getByRole('combobox', { name: 'Month' }))
      .getAllByRole('option')
      .map((option) => option.textContent)
    expect(months).toEqual([
      'Select',
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
    ])
  })

  it('reports the chosen date', async () => {
    const user = userEvent.setup()
    render(<Field />)
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Year' }),
      '1988',
    )
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Month' }),
      '2',
    )
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Day' }),
      '13',
    )
    expect(screen.getByRole('status')).toHaveTextContent('1988-2-13')
  })

  it.each(Object.entries(DATE_MESSAGES) as [DateCode, string][])(
    'links the %s message to every box',
    (code, message) => {
      render(<Field error={code} />)
      expect(screen.getByText(message)).toBeVisible()
      for (const box of screen.getAllByRole('combobox')) {
        expect(box).toHaveAttribute('aria-invalid', 'true')
        expect(box).toHaveAccessibleDescription(message)
      }
    },
  )

  it('passes the ref to the year box', () => {
    const ref = { current: null as HTMLSelectElement | null }
    render(<BirthDateField ref={ref} value={EMPTY} onChange={() => {}} />)
    expect(ref.current).toBe(screen.getByRole('combobox', { name: 'Year' }))
  })
})
