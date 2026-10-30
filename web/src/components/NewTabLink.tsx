import type { ReactNode } from 'react'
import { PRIVACY_PAGE } from '../consent/consent'
import { isHttpUrl } from '../media/isDisplayable'

type Props = { href: string; children: ReactNode }

// The only `target="_blank"` link in the app. The result screen is not in
// the address, so leaving it in the same tab would lose the result. Screen
// readers hear "(opens in a new tab)". Non-http(s) values stay plain text,
// except our own privacy page (that one path only, never other relative
// paths).
export function NewTabLink({ href, children }: Props) {
  if (!isHttpUrl(href) && href !== PRIVACY_PAGE) return <>{children}</>
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
      <span className="visually-hidden"> (opens in a new tab)</span>
    </a>
  )
}
