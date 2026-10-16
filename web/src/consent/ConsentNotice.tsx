import { useId, type Ref } from 'react'
import { NewTabLink } from '../components/NewTabLink'
import { YOUTUBE_TERMS_URL } from '../external/urls'
import { CONSENT_HINT, PRIVACY_PAGE, useConsentState } from './consent'
import styles from './Consent.module.css'

type Props = {
  // The checkbox, so the caller can move focus to it.
  ref?: Ref<HTMLInputElement>
  // Show "agree first" (only while not agreed).
  hint?: boolean
  onToggle?: () => void
}

// The required agreement before the first feature (YouTube API policy
// III.A.1 and III.A.2). M2b moves it in front of the real form's submit.
export function ConsentNotice({ ref, hint = false, onToggle }: Props) {
  const { agreed, setAgreed } = useConsentState()
  const id = useId()
  const showHint = hint && !agreed
  return (
    <div className={styles.notice}>
      <p className={styles.check}>
        <input
          ref={ref}
          id={`${id}-check`}
          type="checkbox"
          className={styles.box}
          required
          checked={agreed}
          aria-describedby={showHint ? `${id}-hint` : undefined}
          onChange={(event) => {
            setAgreed(event.currentTarget.checked)
            onToggle?.()
          }}
        />
        <label htmlFor={`${id}-check`}>
          개인정보처리방침에 동의합니다 (필수)
        </label>
      </p>
      <p>
        <NewTabLink href={PRIVACY_PAGE}>개인정보처리방침 보기</NewTabLink>
      </p>
      <p>
        이 서비스는 YouTube API 서비스를 사용합니다. 이 서비스를 이용하면{' '}
        <NewTabLink href={YOUTUBE_TERMS_URL}>YouTube 이용약관</NewTabLink>에
        동의하게 됩니다.
      </p>
      {showHint && (
        <p id={`${id}-hint`} className={styles.hint}>
          {CONSENT_HINT}
        </p>
      )}
    </div>
  )
}
