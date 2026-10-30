import type { Media } from './types'

// Same text as the server's `CommonsFileMapper.NO_LICENSE`; change both
// together (plan C1 S1, risk 3).
export const LICENSE_UNKNOWN = 'Licence not verified'
export const PROVIDER_TERMS = "Provider's terms apply"

const licenseName = (media: Media) =>
  typeof media.license === 'string' ? media.license.trim() : ''

// The server's code for "the provider's terms apply" (YouTube, M3a5).
export const isProviderTerms = (media: Media) =>
  licenseName(media) === 'provider-terms'

// The license name as the source wrote it, or an honest "unknown". A license
// that is not a string counts as missing (M3b3 decision 2 (b)). The code
// `provider-terms` is put into words (M3b3c).
export function licenseLabel(media: Media): string {
  const name = licenseName(media)
  if (media.license_status === 'unknown' || !name) return LICENSE_UNKNOWN
  return isProviderTerms(media) ? PROVIDER_TERMS : name
}

// Case ignored, any run of whitespace read as one space.
const normalize = (value: string) =>
  value.replace(/\s+/g, ' ').trim().toLowerCase()

const escape = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

// M3b1b review (b): no separate license line when the attribution already
// names the license as a whole token ("CC BY" is not in "CC BY-SA 4.0").
// "Licence not verified" is never hidden: the honest label comes first.
export function licenseInAttribution(media: Media): boolean {
  const label = licenseLabel(media)
  if (label === LICENSE_UNKNOWN) return false
  const token = escape(normalize(label))
  return new RegExp(
    `(^|[^\\p{L}\\p{N}-])${token}($|[^\\p{L}\\p{N}-])`,
    'u',
  ).test(normalize(media.attribution))
}
