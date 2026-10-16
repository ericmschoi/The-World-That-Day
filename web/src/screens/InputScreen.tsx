import { useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { useConsentState } from '../consent/consent'
import { ConsentNotice } from '../consent/ConsentNotice'
import styles from './Screen.module.css'

type Props = { onShowStory: (date: string) => void }

// Demo dates from product brief 12, then two sample dates. M2b removes these
// buttons.
const DEMO_DATES = [
  { date: '1988-09-17', label: '1988년 9월 17일로 보기' },
  { date: '2002-06-22', label: '2002년 6월 22일로 보기' },
  { date: '1995-03-15', label: '1995년 3월 15일로 보기' },
  // Temporary sample dates of public historical events (CLAUDE.md 7.6), for
  // checking video on the real server: a Commons video after M3a4b (Apollo
  // 11) and a YouTube one after M3b3c.
  { date: '1969-07-20', label: '표본: 1969년 7월 20일로 보기' },
  { date: '1986-09-20', label: '표본: 1986년 9월 20일로 보기' },
]

// Placeholder until the real input form arrives in M2b. Before agreeing, the
// buttons stay focusable (aria-disabled, not disabled) and a press explains
// why and moves focus to the checkbox instead of opening the story.
export function InputScreen({ onShowStory }: Props) {
  const { agreed } = useConsentState()
  const [hint, setHint] = useState(false)
  const checkbox = useRef<HTMLInputElement>(null)

  const show = (date: string) => {
    if (agreed) return onShowStory(date)
    // Render the hint first, so it is read as the checkbox's description.
    flushSync(() => setHint(true))
    checkbox.current?.focus()
  }

  return (
    <>
      <h1 className={styles.title}>The World That Day</h1>
      <p className={styles.lead}>
        생년월일과 태어난 장소를 입력하는 화면이 곧 이곳에 생깁니다.
      </p>
      <ConsentNotice
        ref={checkbox}
        hint={hint}
        onToggle={() => setHint(false)}
      />
      <div className={styles.buttons}>
        {DEMO_DATES.map(({ date, label }) => (
          <button
            key={date}
            type="button"
            className={styles.button}
            aria-disabled={agreed ? undefined : true}
            onClick={() => show(date)}
          >
            {label}
          </button>
        ))}
      </div>
    </>
  )
}
