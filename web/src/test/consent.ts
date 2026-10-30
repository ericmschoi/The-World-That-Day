import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'

export const CONSENT_LABEL = 'I agree to the privacy policy (required)'

export const consentBox = () =>
  screen.getByRole('checkbox', { name: CONSENT_LABEL })

// Ticks the privacy consent on the input screen (plan M3b3b T2 (8)).
export async function agree(user: UserEvent) {
  await user.click(consentBox())
}
