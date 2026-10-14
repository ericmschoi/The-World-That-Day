import type { ReactNode } from 'react'
import { isHttpUrl } from '../media/isDisplayable'

type Props = { href: string; children: ReactNode }

// The only `target="_blank"` link in the app. The result screen is not in
// the address, so leaving it in the same tab would lose the result. Screen
// readers hear "(새 창에서 열림)". Non-http(s) values stay plain text.
export function NewTabLink({ href, children }: Props) {
  if (!isHttpUrl(href)) return <>{children}</>
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
      <span className="visually-hidden"> (새 창에서 열림)</span>
    </a>
  )
}
