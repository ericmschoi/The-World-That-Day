// M3b1 CSS rules as tests (docs/plans/m3b1-design-foundation.md, T1 (2)·(5),
// commands C1, C2, C5, and C6 for CSS). Vitest passes `.css?raw` through (vite.config.ts), so
// CSS Modules are read here as real text. C3 (px spacing) stays a human check.
import { describe, expect, it } from 'vitest'

const cssFiles = import.meta.glob<string>('/src/**/*.css', {
  query: '?raw',
  import: 'default',
  eager: true,
})
const TOKENS = '/src/styles/tokens.css'
const tokensCss = cssFiles[TOKENS]
const others = Object.keys(cssFiles).filter((path) => path !== TOKENS)

// Lines of every CSS file except tokens.css that match `pattern`.
const offending = (pattern: RegExp, allow?: RegExp) =>
  others.flatMap((path) =>
    cssFiles[path]
      .split('\n')
      .map((line, i) => ({ line: line.trim(), at: `${path}:${i + 1}` }))
      .filter(({ line }) => pattern.test(line) && !allow?.test(line))
      .map(({ line, at }) => `${at} ${line}`),
  )

describe('CSS files are read as real text', () => {
  it('includes CSS Modules and tokens.css, none blank', () => {
    expect(Object.keys(cssFiles)).toEqual(
      expect.arrayContaining([
        TOKENS,
        '/src/styles/global.css',
        '/src/story/StorySection.module.css',
        '/src/screens/Screen.module.css',
        '/src/App.module.css',
      ]),
    )
    const blank = Object.keys(cssFiles).filter(
      (path) => cssFiles[path].trim() === '',
    )
    expect(blank).toEqual([])
  })
})

describe('M3b1 CSS rules', () => {
  it('C1: colour values live only in tokens.css', () => {
    expect(offending(/#[0-9a-f]{3,8}\b/i)).toEqual([])
    expect(offending(/\b(rgba?|hsla?)\(/i)).toEqual([])
  })

  it('C2: font sizes come from tokens only', () => {
    expect(offending(/font-size\s*:/, /var\(--/)).toEqual([])
  })

  it('C5: photos get no filter or crop', () => {
    const all = Object.keys(cssFiles).flatMap((path) =>
      /filter\s*:|object-fit\s*:\s*cover|sepia|grayscale/i.test(cssFiles[path])
        ? [path]
        : [],
    )
    expect(all).toEqual([])
  })

  it('C6: no absolute http(s) URLs in any CSS file', () => {
    const withUrl = Object.keys(cssFiles).filter((path) =>
      /https?:\/\//.test(cssFiles[path]),
    )
    expect(withUrl).toEqual([])
  })

  it('every var(--...) used is defined in tokens.css', () => {
    const defined = new Set(
      [...tokensCss.matchAll(/(--[\w-]+)\s*:/g)].map((m) => m[1]),
    )
    const undefinedUses = others.flatMap((path) =>
      [...cssFiles[path].matchAll(/var\((--[\w-]+)/g)]
        .map((m) => m[1])
        .filter((name) => !defined.has(name))
        .map((name) => `${path} ${name}`),
    )
    expect(undefinedUses).toEqual([])
  })
})

describe('StorySection chapter layout (T1 (5))', () => {
  const css = cssFiles['/src/story/StorySection.module.css']
  const rule = (selector: string) =>
    new RegExp(`\\.${selector}\\s*\\{([^}]*)\\}`).exec(css)?.[1] ?? ''

  it('opens each chapter with a thin token-coloured rule', () => {
    expect(rule('section')).toMatch(
      /border-top:\s*var\(--border-width\)\s+solid\s+var\(--color-border\)/,
    )
  })

  it('leaves --space-8 between chapters', () => {
    expect(rule('section')).toMatch(/margin-bottom:\s*var\(--space-8\)/)
  })

  it('sets the chapter title in the h2 size token', () => {
    expect(rule('title')).toMatch(/font-size:\s*var\(--font-size-h2\)/)
  })
})

describe('visually-hidden (M3b2 T2)', () => {
  const css = cssFiles['/src/styles/global.css']
  const rule = /\.visually-hidden\s*\{([^}]*)\}/.exec(css)?.[1] ?? ''

  it('hides text from the eye but keeps it for screen readers', () => {
    expect(rule).toMatch(/position:\s*absolute/)
    expect(rule).toMatch(/overflow:\s*hidden/)
    expect(rule).toMatch(/clip-path:\s*inset\(50%\)/)
    expect(rule).not.toMatch(/display:\s*none|visibility:\s*hidden/)
  })
})
