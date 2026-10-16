import { YOUTUBE_EMBED_ORIGIN } from '../external/urls'
import type { Media } from './types'

const filled = (value: unknown) =>
  typeof value === 'string' && value.trim() !== ''

// URLs end up in `href`/`src`, so only http(s) passes (no `javascript:`).
export const isHttpUrl = (value: unknown): value is string =>
  typeof value === 'string' && /^https?:\/\/\S/i.test(value)

// `video_sources` is null or an array of objects whose `url` is http(s).
// Anything else (a string, `[null]`, `[42]`) makes the record not displayable.
function videoSourcesSafe(value: unknown): boolean {
  if (value == null) return true
  if (!Array.isArray(value)) return false
  return value.every(
    (source: unknown) =>
      typeof source === 'object' &&
      source !== null &&
      isHttpUrl((source as { url?: unknown }).url),
  )
}

const LICENSE_STATUSES: readonly unknown[] = ['free', 'other', 'unknown']

// Checks the server's normalization once more (M3a3 rule R1 and the required
// fields of each display mode). A record that fails is hidden, never turned
// into a link. `license` may be null: the caption says "라이선스 확인 안 됨".
// Capture time may be unknown (PM decision 2026-10-11): the caption then says
// "촬영 시점 미상" with no badge, so it is never shown as that day's.
export function isDisplayable(media: Media): boolean {
  if (typeof media !== 'object' || media === null) return false
  const hasCaption =
    [
      media.id,
      media.type,
      media.display_mode,
      media.source_name,
      media.attribution,
      media.retrieved_at,
    ].every(filled) &&
    isHttpUrl(media.source_url) &&
    LICENSE_STATUSES.includes(media.license_status)
  // M3a3 R9. `license_url` is left out on purpose: a bad one only drops the
  // "라이선스" link in the caption, the card itself is still shown.
  const urlsSafe = [media.url, media.thumbnail_url, media.embed_url].every(
    (value) => value == null || isHttpUrl(value),
  )
  const sourcesSafe = videoSourcesSafe(media.video_sources)
  const hasPlace =
    (Number.isFinite(media.lat) && Number.isFinite(media.lng)) ||
    filled(media.place_ref)
  if (!hasCaption || !urlsSafe || !sourcesSafe || !hasPlace) return false

  switch (media.display_mode) {
    case 'image':
      return filled(media.url) && filled(media.thumbnail_url)
    case 'image_embed':
    case 'video_embed':
      return filled(media.embed_url) && filled(media.provider)
    case 'video_file':
      // `sourcesSafe` already guarantees an array of valid entries here.
      return filled(media.url) || (media.video_sources?.length ?? 0) > 0
    case 'sdk':
      return media.render != null && filled(media.provider)
    default:
      // `link_only` (not produced in M3) and unknown modes are not shown.
      return false
  }
}

// Only the privacy-enhanced player the policy names, under IFrame API
// control, never autoplaying on its own (M3b3c). Never built or fixed here.
export function isYouTubeEmbed(media: Media): boolean {
  if (media.provider !== 'youtube' || typeof media.embed_url !== 'string') {
    return false
  }
  try {
    const url = new URL(media.embed_url)
    return (
      url.origin === YOUTUBE_EMBED_ORIGIN &&
      url.pathname.startsWith('/embed/') &&
      url.searchParams.get('enablejsapi') === '1' &&
      !url.searchParams.has('autoplay')
    )
  } catch {
    return false
  }
}

// Displayable and in a mode `MediaItem` actually draws (M3b3). Screens count
// media with this, so a group never keeps a heading over nothing. YouTube
// only after the reader agreed to the privacy policy (M3b3c).
export function canRender(media: Media, consented = false): boolean {
  if (!isDisplayable(media)) return false
  if (media.display_mode === 'video_embed') {
    return consented && isYouTubeEmbed(media)
  }
  return media.display_mode === 'image' || media.display_mode === 'video_file'
}
