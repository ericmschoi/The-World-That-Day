import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { ApiError, ApiTimeoutError } from '../api/client'
import { server } from '../mocks/server'
import { getWorld } from './api'

function problem(status: number, code: string) {
  return HttpResponse.json(
    {
      type: 'about:blank',
      title: status === 400 ? 'Bad Request' : 'Service Unavailable',
      status,
      detail: 'A fixed sentence without request values.',
      instance: '/api/story/world',
      code,
    },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

describe('getWorld', () => {
  it('requests /api/story/world with the date query', async () => {
    let url: URL | null = null
    server.use(
      http.get('/api/story/world', ({ request }) => {
        url = new URL(request.url)
        return HttpResponse.json({ date: '1988-09-17', events: [] })
      }),
    )

    await expect(getWorld('1988-09-17')).resolves.toEqual({
      date: '1988-09-17',
      events: [],
    })
    expect(url!.pathname).toBe('/api/story/world')
    expect([...url!.searchParams]).toEqual([['date', '1988-09-17']])
  })

  it('encodes the date so it cannot add query parameters', async () => {
    let url: URL | null = null
    server.use(
      http.get('/api/story/world', ({ request }) => {
        url = new URL(request.url)
        return HttpResponse.json({})
      }),
    )

    await getWorld('1988-09-17&x=1')

    expect([...url!.searchParams]).toEqual([['date', '1988-09-17&x=1']])
  })

  it('gives ApiError 400 with code DATE_NOT_EXIST for a date that does not exist', async () => {
    server.use(
      http.get('/api/story/world', () => problem(400, 'DATE_NOT_EXIST')),
    )

    const error = await getWorld('2001-02-29').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).code).toBe('DATE_NOT_EXIST')
    expect((error as ApiError).message).toBe('GET /api/story/world failed: 400')
    expect((error as ApiError).message).not.toContain('2001-02-29')
  })

  it('gives ApiError 503 with code UPSTREAM_UNAVAILABLE', async () => {
    server.use(
      http.get('/api/story/world', () => problem(503, 'UPSTREAM_UNAVAILABLE')),
    )

    const error = await getWorld('1988-09-17').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(503)
    expect((error as ApiError).code).toBe('UPSTREAM_UNAVAILABLE')
    expect((error as ApiError).message).not.toContain('1988-09-17')
  })

  it('gives code null for a 500 whose body is not JSON', async () => {
    server.use(
      http.get(
        '/api/story/world',
        () => new HttpResponse('Internal Server Error', { status: 500 }),
      ),
    )

    const error = await getWorld('1988-09-17').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(500)
    expect((error as ApiError).code).toBeNull()
    expect((error as ApiError).message).not.toContain('1988-09-17')
  })

  it('times out without the date in the message', async () => {
    server.use(
      http.get('/api/story/world', async () => {
        await delay('infinite')
        return HttpResponse.json({})
      }),
    )

    const error = await getWorld('1988-09-17', undefined, 20).catch(
      (e: unknown) => e,
    )

    expect(error).toBeInstanceOf(ApiTimeoutError)
    expect((error as Error).message).not.toContain('1988-09-17')
  })

  it('passes the caller signal through', async () => {
    const controller = new AbortController()
    controller.abort()
    server.use(
      http.get('/api/story/world', async () => {
        await delay('infinite')
        return HttpResponse.json({})
      }),
    )

    const error = await getWorld('2002-06-22', controller.signal).catch(
      (e: unknown) => e,
    )

    expect((error as Error).name).toBe('AbortError')
  })
})
