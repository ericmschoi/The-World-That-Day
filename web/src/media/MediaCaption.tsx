import { NewTabLink } from '../components/NewTabLink'
import { dateLabel } from './dateLabel'
import { isHttpUrl } from './isDisplayable'
import {
  isProviderTerms,
  licenseInAttribution,
  licenseLabel,
} from './licenseLabel'
import type { Media } from './types'
import styles from './MediaCard.module.css'

type Props = { media: Media }

// The caption every media card carries (product brief 6, CLAUDE.md 7.1):
// date relation first, then description, source, license, and links.
// An unknown capture time gets no badge, only "Capture time unknown". The
// license line is left out when the attribution already names the license.
export function MediaCaption({ media }: Props) {
  const { badge, text } = dateLabel(media)
  // Not a string (a number, an object): no description line (M3b3 decision 2 (b)).
  const description =
    typeof media.description === 'string' ? media.description.trim() : ''
  return (
    <figcaption className={styles.caption}>
      <p className={styles.when}>
        {badge && <span className={styles.badge}>{badge}</span>}
        {badge && ' '}
        {text}
      </p>
      {description && <p className={styles.description}>{description}</p>}
      <p className={styles.meta}>{media.attribution}</p>
      {!licenseInAttribution(media) && (
        <p className={styles.meta}>{licenseLabel(media)}</p>
      )}
      <p className={styles.links}>
        <NewTabLink href={media.source_url}>View original</NewTabLink>
        {isHttpUrl(media.license_url) && (
          <>
            <span aria-hidden="true"> · </span>
            <NewTabLink href={media.license_url}>
              {isProviderTerms(media) ? 'Terms' : 'Licence'}
            </NewTabLink>
          </>
        )}
      </p>
    </figcaption>
  )
}
