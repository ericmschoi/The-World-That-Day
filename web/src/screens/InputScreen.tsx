import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { flushSync } from 'react-dom'
import { useConsentState } from '../consent/consent'
import { ConsentNotice } from '../consent/ConsentNotice'
import { BirthDateField } from '../input/BirthDateField'
import { checkDate, type DateCode, type DateParts } from '../input/dateRules'
import formStyles from '../input/Form.module.css'
import { checkPlace, PLACE_MESSAGES, type PlaceCode } from '../input/placeRules'
import styles from './Screen.module.css'

// What the form hands over. The time is for the opening only (never sent).
export type StoryInput = { date: string; place: string; time?: string }

type Props = {
  // The input shown last, when coming back from the story.
  initial?: StoryInput
  onSubmit: (input: StoryInput) => void
}

type Errors = { date?: DateCode; place?: PlaceCode }

const EMPTY: DateParts = { year: '', month: '', day: '' }

// "1988-09-17" as the select boxes' values ("1988", "9", "17").
function toParts(date: string): DateParts {
  const [year, month, day] = date.split('-').map((n) => String(Number(n)))
  return { year, month, day }
}

// The input form (plan M2b). Submit is never blocked: it checks every field,
// puts a message under each wrong one and focuses the first, in DOM order.
// Before agreeing the button stays focusable (aria-disabled, not disabled).
export function InputScreen({ initial, onSubmit }: Props) {
  const { agreed } = useConsentState()
  const [date, setDate] = useState(initial ? toParts(initial.date) : EMPTY)
  const [place, setPlace] = useState(initial?.place ?? '')
  const [time, setTime] = useState(initial?.time ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [hint, setHint] = useState(false)
  const yearBox = useRef<HTMLSelectElement>(null)
  const placeBox = useRef<HTMLInputElement>(null)
  const checkbox = useRef<HTMLInputElement>(null)
  const title = useRef<HTMLHeadingElement>(null)
  const id = useId()
  const back = initial !== undefined

  // Back from the story: start reading at the title again.
  useEffect(() => {
    if (back) title.current?.focus()
  }, [back])

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const day = checkDate(date)
    const where = checkPlace(place)
    // Render the messages first, so each is read as its field's description.
    flushSync(() => {
      setErrors({
        date: 'code' in day ? day.code : undefined,
        place: 'code' in where ? where.code : undefined,
      })
      setHint(!agreed)
    })
    if ('code' in day) return yearBox.current?.focus()
    if ('code' in where) return placeBox.current?.focus()
    if (!agreed) return checkbox.current?.focus()
    onSubmit({ date: day.date, place: where.place, time: time || undefined })
  }

  const placeError = errors.place && PLACE_MESSAGES[errors.place]
  return (
    <>
      <h1 ref={title} className={styles.title} tabIndex={-1}>
        The World That Day
      </h1>
      <form noValidate autoComplete="off" onSubmit={submit}>
        <BirthDateField
          ref={yearBox}
          value={date}
          onChange={setDate}
          error={errors.date}
        />
        <div className={formStyles.field}>
          <label htmlFor={`${id}-place`} className={formStyles.label}>
            태어난 장소 (필수)
          </label>
          <input
            ref={placeBox}
            id={`${id}-place`}
            type="text"
            className={`${formStyles.control} ${formStyles.wide}`}
            autoComplete="off"
            value={place}
            aria-invalid={placeError ? true : undefined}
            aria-describedby={`${placeError ? `${id}-place-error ` : ''}${id}-place-hint`}
            onChange={(event) => setPlace(event.currentTarget.value)}
          />
          {placeError && (
            <p id={`${id}-place-error`} className={formStyles.error}>
              {placeError}
            </p>
          )}
          <p id={`${id}-place-hint`} className={formStyles.hint}>
            예: 서울, 광주 서구 풍암동, 서울대학교병원
          </p>
        </div>
        <div className={formStyles.field}>
          <label htmlFor={`${id}-time`} className={formStyles.label}>
            출생 시각 (선택)
          </label>
          <input
            id={`${id}-time`}
            type="time"
            className={formStyles.control}
            autoComplete="off"
            value={time}
            aria-describedby={`${id}-time-hint ${id}-time-partial`}
            onChange={(event) => setTime(event.currentTarget.value)}
          />
          <p id={`${id}-time-hint`} className={formStyles.hint}>
            오프닝 문구에만 씁니다. 서버로 보내지 않습니다.
          </p>
          <p id={`${id}-time-partial`} className={formStyles.hint}>
            시와 분을 모두 넣어야 반영됩니다.
          </p>
        </div>
        <ConsentNotice
          ref={checkbox}
          hint={hint}
          onToggle={() => setHint(false)}
        />
        <button
          type="submit"
          className={styles.button}
          aria-disabled={agreed ? undefined : true}
        >
          그날 보기
        </button>
      </form>
    </>
  )
}
