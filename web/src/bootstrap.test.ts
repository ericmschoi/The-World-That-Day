// Contract checks for the M1b bootstrap (docs/plans/m1b-bootstrap-web.md, T1, C1, C2).
// Files are read as raw text through Vite so the app tsconfig needs no Node types.
// T4 (.github/workflows/web-ci.yml) is outside Vite's allowed fs root, so it is
// checked by the T5 report commands instead.
import { describe, expect, it } from 'vitest'
import indexHtml from '../index.html?raw'
import nvmrc from '../.nvmrc?raw'
import packageJsonRaw from '../package.json?raw'
import viteConfig from '../vite.config.ts?raw'

// Every file under src (lazy: only the paths are used).
const allPaths = Object.keys(import.meta.glob('/src/**/*'))
// Vitest stubs `*.module.css` even with `?raw`, so CSS Modules are left to the
// C1/C2 grep commands in the plan; plain CSS and all code are read here.
const sourceFiles = import.meta.glob<string>(
  ['/src/**/*.{ts,tsx,css}', '!/src/**/*.module.css'],
  { query: '?raw', import: 'default', eager: true },
)
const isTestFile = (path: string) => /\.test\.tsx?$/.test(path)

describe('T1 scaffold', () => {
  const pkg = JSON.parse(packageJsonRaw) as {
    name: string
    private: boolean
    engines?: { node?: string }
    scripts: Record<string, string>
  }

  it('uses the project package name and stays private', () => {
    expect(pkg.name).toBe('the-world-that-day')
    expect(pkg.private).toBe(true)
    expect(pkg.engines?.node).toBe('>=24')
  })

  it('has exactly the nine scripts from the plan', () => {
    expect(pkg.scripts).toEqual({
      dev: 'vite',
      build: 'tsc -b && vite build',
      preview: 'vite preview',
      lint: 'eslint .',
      format: 'prettier --write .',
      'format:check': 'prettier --check .',
      typecheck: 'tsc -b',
      test: 'vitest run',
      'test:watch': 'vitest',
    })
  })

  it('declares Korean language, the service title and a viewport', () => {
    expect(indexHtml.match(/<html lang="ko">/g)).toHaveLength(1)
    expect(indexHtml.match(/<title>The World That Day<\/title>/g)).toHaveLength(
      1,
    )
    expect(indexHtml).toMatch(
      /<meta name="viewport" content="width=device-width, initial-scale=1(\.0)?"/,
    )
  })

  it('pins the dev server to 5173 and proxies /api to port 8080', () => {
    expect(viteConfig).toMatch(/\bport:\s*5173\b/)
    expect(viteConfig).toMatch(/\bstrictPort:\s*true\b/)
    expect(viteConfig).toMatch(
      /['"]\/api['"]\s*:\s*['"]http:\/\/localhost:8080['"]/,
    )
  })

  it('pins Node 24 in .nvmrc', () => {
    expect(nvmrc.trim()).toBe('24')
  })

  it('has no template demo code left', () => {
    expect(allPaths.filter((p) => p.startsWith('/src/assets/'))).toEqual([])
    expect(allPaths).not.toContain('/src/App.css')
    // Built from parts so this file itself does not match the T1 (7) grep.
    const counterText = ['count', 'is'].join(' ')
    const withCounter = Object.keys(sourceFiles).filter((p) =>
      sourceFiles[p].includes(counterText),
    )
    expect(withCounter).toEqual([])
  })
})

describe('C1/C2 product code rules', () => {
  it('C1: reads no build-time environment variables', () => {
    // Built from parts so this file itself does not match the C1 grep.
    const envAccess = new RegExp(['import', 'meta', 'env'].join('\\.'))
    const offenders = Object.keys(sourceFiles).filter((p) =>
      envAccess.test(sourceFiles[p]),
    )
    expect(offenders).toEqual([])
  })

  // Mock data is test-only, as in the M3b1 plan's C6 check.
  it('C2: has no absolute http(s) URLs outside test files and mocks', () => {
    const url = /https?:\/\//
    const offenders = Object.keys(sourceFiles).filter(
      (p) =>
        !isTestFile(p) &&
        !p.startsWith('/src/mocks/') &&
        url.test(sourceFiles[p]),
    )
    expect(offenders).toEqual([])
    expect(indexHtml).not.toMatch(url)
  })
})
