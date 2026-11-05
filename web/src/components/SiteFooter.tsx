import { PRIVACY_PAGE } from '../consent/consent'
import { NewTabLink } from './NewTabLink'

type Props = { className?: string }

// Shared by the input and result screens, outside `<main>`, so the privacy
// policy is always one link away (YouTube API policy III.A.2 (a)).
export function SiteFooter({ className }: Props) {
  return (
    <footer className={className}>
      <NewTabLink href={PRIVACY_PAGE}>Privacy policy</NewTabLink>
    </footer>
  )
}
