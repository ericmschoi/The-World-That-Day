// WCAG 2.x contrast ratio, used by tests to keep token colours readable.
// Definition: WCAG 2.1, "contrast ratio" and "relative luminance".

const HEX = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i

function channels(hex: string): [number, number, number] {
  const match = HEX.exec(hex.trim())
  if (!match) throw new Error(`Not a #rgb or #rrggbb colour: ${hex}`)
  const digits =
    match[1].length === 3 ? [...match[1]].map((d) => d + d).join('') : match[1]
  return [0, 2, 4].map((i) => parseInt(digits.slice(i, i + 2), 16)) as [
    number,
    number,
    number,
  ]
}

export function relativeLuminance(hex: string): number {
  const [r, g, b] = channels(hex).map((value) => {
    const c = value / 255
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

export function contrastRatio(a: string, b: string): number {
  const [light, dark] = [relativeLuminance(a), relativeLuminance(b)].sort(
    (x, y) => y - x,
  )
  return (light + 0.05) / (dark + 0.05)
}

// Reads `--name: #hex;` declarations from a CSS source.
export function readColorTokens(css: string): Record<string, string> {
  const tokens: Record<string, string> = {}
  for (const m of css.matchAll(/(--color-[\w-]+)\s*:\s*(#[0-9a-f]{3,6})\b/gi)) {
    tokens[m[1]] = m[2]
  }
  return tokens
}
