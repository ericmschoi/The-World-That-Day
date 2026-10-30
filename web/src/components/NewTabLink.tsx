import type { ReactNode } from 'react'
import { PRIVACY_PAGE } from '../consent/consent'
import { isHttpUrl } from '../media/isDisplayable'

type Props = { href: string; lang?: string; children: ReactNode }

// The page language (`<html lang>` in index.html and privacy.html).
const PAGE_LANG = 'en-CA'

// The only `target="_blank"` link in the app. The result screen is not in
// the address, so leaving it in the same tab would lose the result. Screen
// readers hear "(opens in a new tab)". Non-http(s) values stay plain text,
// except our own privacy page (that one path only, never other relative
// paths). `lang` marks text in another language (a Korean page title); the
// hidden hint stays in the page language.
export function NewTabLink({ href, lang, children }: Props) {
  if (!isHttpUrl(href) && href !== PRIVACY_PAGE) {
    return lang ? <span lang={lang}>{children}</span> : <>{children}</>
  }
  return (
    <a href={href} target="_blank" rel="noopener noreferrer" lang={lang}>
      {children}
      <span className="visually-hidden" lang={lang && PAGE_LANG}>
        {' '}
        (opens in a new tab)
      </span>
    </a>
  )
}
