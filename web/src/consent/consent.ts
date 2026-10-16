import { createContext, useContext } from 'react'

// The privacy policy page: a second Vite HTML entry (web/privacy.html).
export const PRIVACY_PAGE = '/privacy.html'

// Shown when a button is pressed before agreeing.
export const CONSENT_HINT = '개인정보처리방침에 동의해야 볼 수 있습니다.'

// Agreement to the privacy policy, in memory only: never in the URL, cookies
// or browser storage, so a reload asks again (YouTube API policy III.A.2).
export type Consent = { agreed: boolean; setAgreed(agreed: boolean): void }

export const ConsentContext = createContext<Consent | null>(null)

const OUTSIDE: Consent = { agreed: false, setAgreed: () => {} }

// The nearest provider's state. Outside any provider nothing is agreed.
export function useConsentState(): Consent {
  return useContext(ConsentContext) ?? OUTSIDE
}

// Whether the reader agreed. M3b3c draws YouTube videos only when true, so a
// path that skips the input screen (no provider) shows none.
export function useConsent(): boolean {
  return useConsentState().agreed
}
