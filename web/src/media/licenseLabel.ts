import type { Media } from './types'

export const LICENSE_UNKNOWN = '라이선스 확인 안 됨'

// The license name as the source wrote it, or an honest "unknown".
export function licenseLabel(media: Media): string {
  const name = media.license?.trim()
  return media.license_status === 'unknown' || !name ? LICENSE_UNKNOWN : name
}
