import type { Media } from './types'

const filled = (value: unknown) =>
  typeof value === 'string' && value.trim() !== ''

const LICENSE_STATUSES: readonly unknown[] = ['free', 'other', 'unknown']

// Checks the server's normalization once more (M3a3 rule R1 and the required
// fields of each display mode). A record that fails is hidden, never turned
// into a link. `license` may be null: the caption says "라이선스 확인 안 됨".
// Capture time must exist (`captured_at` or `current`): CLAUDE.md 7.1 and
// product brief 6.2. The M3b1 plan says otherwise; waiting on a PM decision.
export function isDisplayable(media: Media): boolean {
  const hasCaption =
    [
      media.id,
      media.type,
      media.display_mode,
      media.source_name,
      media.source_url,
      media.attribution,
      media.retrieved_at,
    ].every(filled) && LICENSE_STATUSES.includes(media.license_status)
  const hasPlace =
    (Number.isFinite(media.lat) && Number.isFinite(media.lng)) ||
    filled(media.place_ref)
  const hasCaptureTime =
    filled(media.captured_at) || media.date_precision === 'current'
  if (!hasCaption || !hasPlace || !hasCaptureTime) return false

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
