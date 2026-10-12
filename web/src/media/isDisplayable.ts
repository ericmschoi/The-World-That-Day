import type { Media } from './types'

const filled = (value: unknown) =>
  typeof value === 'string' && value.trim() !== ''

// URLs end up in `href`/`src`, so only http(s) passes (no `javascript:`).
const httpUrl = (value: unknown) =>
  typeof value === 'string' && /^https?:\/\/\S/i.test(value)

const LICENSE_STATUSES: readonly unknown[] = ['free', 'other', 'unknown']

// Checks the server's normalization once more (M3a3 rule R1 and the required
// fields of each display mode). A record that fails is hidden, never turned
// into a link. `license` may be null: the caption says "라이선스 확인 안 됨".
// Capture time may be unknown (PM decision 2026-10-11): the caption then says
// "촬영 시점 미상" with no badge, so it is never shown as that day's.
export function isDisplayable(media: Media): boolean {
  const hasCaption =
    [
      media.id,
      media.type,
      media.display_mode,
      media.source_name,
      media.attribution,
      media.retrieved_at,
    ].every(filled) &&
    httpUrl(media.source_url) &&
    LICENSE_STATUSES.includes(media.license_status)
  const urlsSafe = [
    media.url,
    media.thumbnail_url,
    media.embed_url,
    media.license_url,
    ...(media.video_sources ?? []).map((source) => source.url),
  ].every((value) => value == null || httpUrl(value))
  const hasPlace =
    (Number.isFinite(media.lat) && Number.isFinite(media.lng)) ||
    filled(media.place_ref)
  if (!hasCaption || !urlsSafe || !hasPlace) return false

  switch (media.display_mode) {
    case 'image':
      return filled(media.url) && filled(media.thumbnail_url)
    case 'image_embed':
    case 'video_embed':
      return filled(media.embed_url) && filled(media.provider)
    case 'video_file':
      return filled(media.url) || (media.video_sources?.length ?? 0) > 0
    case 'sdk':
      return media.render != null && filled(media.provider)
    default:
      // `link_only` (not produced in M3) and unknown modes are not shown.
      return false
  }
}
