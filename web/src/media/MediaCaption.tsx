import { NewTabLink } from '../components/NewTabLink'
import { dateLabel } from './dateLabel'
import { isHttpUrl } from './isDisplayable'
import { licenseInAttribution, licenseLabel } from './licenseLabel'
import type { Media } from './types'
import styles from './MediaCard.module.css'

type Props = { media: Media }

// The caption every media card carries (product brief 6, CLAUDE.md 7.1):
// date relation first, then description, source, license, and links.
// An unknown capture time gets no badge, only "촬영 시점 미상". The license
// line is left out when the attribution already names the license.
export function MediaCaption({ media }: Props) {
  const { badge, text } = dateLabel(media)
  const description = media.description?.trim()
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
        <NewTabLink href={media.source_url}>원문 보기</NewTabLink>
        {isHttpUrl(media.license_url) && (
          <>
            <span aria-hidden="true"> · </span>
            <NewTabLink href={media.license_url}>라이선스</NewTabLink>
          </>
        )}
      </p>
    </figcaption>
  )
}
