import { useId, type Ref } from 'react'
import {
  DATE_MESSAGES,
  yearOptions,
  type DateCode,
  type DateParts,
} from './dateRules'
import styles from './BirthDateField.module.css'

type Props = {
  value: DateParts
  onChange: (value: DateParts) => void
  // The rule the date broke on the last submit, if any.
  error?: DateCode
  // The year box, so the form can move focus to it.
  ref?: Ref<HTMLSelectElement>
}

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1)
// Every month shows 31 days; a missing date is refused with a message.
const DAYS = Array.from({ length: 31 }, (_, i) => i + 1)

// The birth date as three select boxes (plan M2b, decision M2-P6 (a)).
export function BirthDateField({ value, onChange, error, ref }: Props) {
  const id = useId()
  const errorId = `${id}-error`
  const boxes = [
    { key: 'year', label: '년', options: yearOptions() },
    { key: 'month', label: '월', options: MONTHS },
    { key: 'day', label: '일', options: DAYS },
  ] as const

  return (
    <fieldset className={styles.field}>
      <legend className={styles.legend}>생년월일 (필수)</legend>
      <div className={styles.boxes}>
        {boxes.map(({ key, label, options }) => (
          <span key={key} className={styles.box}>
            <select
              ref={key === 'year' ? ref : undefined}
              id={`${id}-${key}`}
              className={styles.select}
              value={value[key]}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? errorId : undefined}
              onChange={(event) =>
                onChange({ ...value, [key]: event.currentTarget.value })
              }
            >
              <option value="">선택</option>
              {options.map((n) => (
                <option key={n} value={String(n)}>
                  {n}
                </option>
              ))}
            </select>
            <label htmlFor={`${id}-${key}`}>{label}</label>
          </span>
        ))}
      </div>
      {error && (
        <p id={errorId} className={styles.error}>
          {DATE_MESSAGES[error]}
        </p>
      )}
    </fieldset>
  )
}
