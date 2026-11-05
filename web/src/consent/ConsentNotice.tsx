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
// The two-line summary above the checkbox is S-9 of canada-privacy.md 10.1;
// change it with the policy when the sources or what is sent change.
export function ConsentNotice({ ref, hint = false, onToggle }: Props) {
  const { agreed, setAgreed } = useConsentState()
  const id = useId()
  const showHint = hint && !agreed
  return (
    <div className={styles.notice}>
      {/* S-9 (C1 S2): what is sent, who receives it, why, and the risk. */}
      <p>
        Only your date of birth is sent to our server, to find events, photos
        and videos from that day.
      </p>
      <p>
        Photos and videos load straight from Wikimedia and YouTube, which
        receive your IP address and may process it outside Canada.
      </p>
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
          I agree to the privacy policy (required)
        </label>
      </p>
      <p>
        <NewTabLink href={PRIVACY_PAGE}>Read the privacy policy</NewTabLink>
      </p>
      <p>
        This service uses YouTube API Services. By using it, you agree to be
        bound by the{' '}
        <NewTabLink href={YOUTUBE_TERMS_URL}>
          YouTube Terms of Service
        </NewTabLink>
        .
      </p>
      {showHint && (
        <p id={`${id}-hint`} className={styles.hint}>
          {CONSENT_HINT}
        </p>
      )}
    </div>
  )
}
