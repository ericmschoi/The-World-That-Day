// `navigator.connection` is Chromium-only and missing from the DOM typings.
type WithConnection = Navigator & { connection?: { saveData?: boolean } }

// M3-P8: no autoplay when the user asks for reduced motion or data saving;
// the video then waits on its poster and controls. Read again on every call
// so a changed setting applies to the next attempt. Browsers without
// `navigator.connection` (Safari, Firefox) count as not saving data.
export function canAutoplay(): boolean {
  const reducedMotion =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const saveData = (navigator as WithConnection).connection?.saveData === true
  return !reducedMotion && !saveData
}
