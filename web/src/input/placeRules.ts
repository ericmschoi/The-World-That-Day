// Place rules for the input form (plan M2b "웹: 화면과 상태"). The limit is
// the M2a server's: 50 code points after trimming.
export const PLACE_MAX = 50

export type PlaceCode = 'PLACE_REQUIRED' | 'PLACE_TOO_LONG'

export const PLACE_MESSAGES: Record<PlaceCode, string> = {
  PLACE_REQUIRED: 'Enter your place of birth.',
  PLACE_TOO_LONG: 'The place can be up to 50 characters long.',
}

export function checkPlace(
  text: string,
): { place: string } | { code: PlaceCode } {
  const place = text.trim()
  if (place === '') return { code: 'PLACE_REQUIRED' }
  if ([...place].length > PLACE_MAX) return { code: 'PLACE_TOO_LONG' }
  return { place }
}
