import type { Media } from './types'

export const LICENSE_UNKNOWN = '라이선스 확인 안 됨'

// The license name as the source wrote it, or an honest "unknown".
export function licenseLabel(media: Media): string {
  const name = media.license?.trim()
  return media.license_status === 'unknown' || !name ? LICENSE_UNKNOWN : name
}

// Case ignored, any run of whitespace read as one space.
const normalize = (value: string) =>
  value.replace(/\s+/g, ' ').trim().toLowerCase()

const escape = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

// M3b1b review (b): no separate license line when the attribution already
// names the license as a whole token ("CC BY" is not in "CC BY-SA 4.0").
// "라이선스 확인 안 됨" is never hidden: the honest label comes first.
export function licenseInAttribution(media: Media): boolean {
  const label = licenseLabel(media)
  if (label === LICENSE_UNKNOWN) return false
  const token = escape(normalize(label))
  return new RegExp(
    `(^|[^\\p{L}\\p{N}-])${token}($|[^\\p{L}\\p{N}-])`,
    'u',
  ).test(normalize(media.attribution))
}
