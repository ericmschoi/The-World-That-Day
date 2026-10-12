// Design token contract (docs/plans/m3b1-design-foundation.md, T1).
import { describe, expect, it } from 'vitest'
import {
  contrastRatio,
  readColorTokens,
  relativeLuminance,
} from '../test/contrast'
import globalCss from './global.css?raw'
import tokensCss from './tokens.css?raw'

const colors = readColorTokens(tokensCss)
const color = (name: string) => {
  const value = colors[name]
  if (!value) throw new Error(`Missing colour token ${name}`)
  return value
}

describe('contrast helpers', () => {
  it('computes the WCAG extremes and is symmetric', () => {
    expect(relativeLuminance('#000000')).toBe(0)
    expect(relativeLuminance('#ffffff')).toBeCloseTo(1, 10)
    expect(contrastRatio('#000', '#fff')).toBeCloseTo(21, 10)
    expect(contrastRatio('#fff', '#000')).toBeCloseTo(21, 10)
    expect(contrastRatio('#2f4a6d', '#2f4a6d')).toBe(1)
  })

  it('puts colours just below and above the AA line on the right side', () => {
    // #777777 on white is the classic near miss (about 4.48).
    expect(contrastRatio('#777777', '#ffffff')).toBeLessThan(4.5)
    expect(contrastRatio('#767676', '#ffffff')).toBeGreaterThanOrEqual(4.5)
    // Light grey fails even the 3:1 non-text rule.
    expect(contrastRatio('#aaaaaa', '#ffffff')).toBeLessThan(3)
  })

  it('rejects values that are not hex colours', () => {
    expect(() => relativeLuminance('red')).toThrow()
    expect(() => relativeLuminance('#12345')).toThrow()
  })

  it('reads colour tokens from CSS', () => {
    expect(
      readColorTokens(':root { --color-a: #fff; --space-1: 4px; }'),
    ).toEqual({ '--color-a': '#fff' })
  })
})

describe('tokens.css', () => {
  it('defines every token named in the design direction', () => {
    const names = [
      '--color-bg',
      '--color-surface',
      '--color-text',
      '--color-muted',
      '--color-border',
      '--color-accent',
      '--color-focus',
      '--color-tragic-bg',
      '--color-tragic-text',
      '--font-size-caption',
      '--font-size-body',
      '--font-size-lead',
      '--font-size-h2',
      '--font-size-h1',
      '--font-size-display',
      '--line-height-body',
      '--line-height-heading',
      ...[1, 2, 3, 4, 5, 6, 7, 8].map((n) => `--space-${n}`),
      '--radius-card',
      '--duration-short',
      '--duration-base',
      '--easing-standard',
    ]
    const missing = names.filter(
      (name) => !new RegExp(`${name}\\s*:`).test(tokensCss),
    )
    expect(missing).toEqual([])
  })

  it('uses the palette A values from the plan', () => {
    expect(colors).toMatchObject({
      '--color-bg': '#faf7f2',
      '--color-surface': '#ffffff',
      '--color-text': '#1f1d1a',
      '--color-muted': '#5e5850',
      '--color-border': '#e4ddd2',
      '--color-accent': '#2f4a6d',
      '--color-focus': '#0b63ce',
      '--color-tragic-bg': '#eef0f2',
      '--color-tragic-text': '#2b2f33',
    })
  })

  it.each([
    ['--color-text', '--color-bg'],
    ['--color-muted', '--color-bg'],
    ['--color-accent', '--color-bg'],
    ['--color-text', '--color-surface'],
    ['--color-muted', '--color-surface'],
    ['--color-tragic-text', '--color-tragic-bg'],
    ['--color-muted', '--color-tragic-bg'],
    ['--color-on-accent', '--color-accent'],
  ])('%s on %s meets AA for text (4.5:1)', (fg, bg) => {
    expect(contrastRatio(color(fg), color(bg))).toBeGreaterThanOrEqual(4.5)
  })

  it('--color-focus on --color-bg meets the non-text rule (3:1)', () => {
    expect(
      contrastRatio(color('--color-focus'), color('--color-bg')),
    ).toBeGreaterThanOrEqual(3)
  })
})

describe('global.css', () => {
  it('loads the tokens', () => {
    expect(globalCss).toMatch(/@import\s+['"]\.\/tokens\.css['"]/)
  })

  it('turns motion off under prefers-reduced-motion', () => {
    const block =
      /@media\s*\(prefers-reduced-motion:\s*reduce\)\s*\{([\s\S]*)\}/
    const match = block.exec(globalCss)
    expect(match).not.toBeNull()
    expect(match?.[1]).toMatch(/animation-duration/)
    expect(match?.[1]).toMatch(/transition-duration/)
  })
})
