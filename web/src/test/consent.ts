import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'

export const CONSENT_LABEL = '개인정보처리방침에 동의합니다 (필수)'

export const consentBox = () =>
  screen.getByRole('checkbox', { name: CONSENT_LABEL })

// Ticks the privacy consent on the input screen (plan M3b3b T2 (8)).
export async function agree(user: UserEvent) {
  await user.click(consentBox())
}
