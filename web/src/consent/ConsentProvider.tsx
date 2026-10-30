import { useMemo, useState, type ReactNode } from 'react'
import { ConsentContext } from './consent'

// Holds the agreement for the app's lifetime: it survives "Start over" and is
// readable on the result screen, and is gone after a reload.
export function ConsentProvider({ children }: { children: ReactNode }) {
  const [agreed, setAgreed] = useState(false)
  const value = useMemo(() => ({ agreed, setAgreed }), [agreed])
  return <ConsentContext value={value}>{children}</ConsentContext>
}
