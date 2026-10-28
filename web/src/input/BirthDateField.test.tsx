// Plan M2b T2 (3): the birth-date select boxes. Dates are demo dates and
// boundary values only.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BirthDateField } from './BirthDateField'
import { DATE_MESSAGES, type DateCode, type DateParts } from './dateRules'

const EMPTY: DateParts = { year: '', month: '', day: '' }

// Option values, without the leading "선택" placeholder.
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
  it('is a group named 생년월일 (필수) with boxes 년, 월, 일', () => {
    render(<Field />)
    const group = screen.getByRole('group', { name: '생년월일 (필수)' })
    const boxes = within(group).getAllByRole('combobox')
    expect(boxes).toHaveLength(3)
    expect(boxes[0]).toHaveAccessibleName('년')
    expect(boxes[1]).toHaveAccessibleName('월')
    expect(boxes[2]).toHaveAccessibleName('일')
    for (const box of boxes) {
      expect(box).toHaveValue('')
      expect(box).not.toHaveAttribute('aria-invalid')
      expect(box).not.toHaveAccessibleDescription()
    }
  })

  it('lists every year from this year (Asia/Seoul) down to 1950', () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-27T03:00:00Z'))
    render(<Field />)
    const expected = Array.from({ length: 2026 - 1950 + 1 }, (_, i) =>
      String(2026 - i),
    )
    expect(values('년')).toEqual(expected)
  })

  it('lists 12 months and 31 days', () => {
    render(<Field />)
    expect(values('월')).toEqual(
      Array.from({ length: 12 }, (_, i) => String(i + 1)),
    )
    expect(values('일')).toEqual(
      Array.from({ length: 31 }, (_, i) => String(i + 1)),
    )
  })

  it('reports the chosen date', async () => {
    const user = userEvent.setup()
    render(<Field />)
    await user.selectOptions(
      screen.getByRole('combobox', { name: '년' }),
      '1988',
    )
    await user.selectOptions(screen.getByRole('combobox', { name: '월' }), '9')
    await user.selectOptions(screen.getByRole('combobox', { name: '일' }), '17')
    expect(screen.getByRole('status')).toHaveTextContent('1988-9-17')
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
    expect(ref.current).toBe(screen.getByRole('combobox', { name: '년' }))
  })
})
