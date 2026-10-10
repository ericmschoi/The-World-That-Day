// Requests that reached MSW without a matching handler during the current test.
// `src/test/setup.ts` fails the test if any are left at the end of it.
const unhandled: string[] = []

export function recordUnhandledRequest(request: Request): void {
  unhandled.push(`${request.method} ${request.url}`)
}

// Returns the recorded requests ("METHOD URL") and clears the list. Tests that
// send an unmocked request on purpose must assert on the result.
export function takeUnhandledRequests(): string[] {
  return unhandled.splice(0)
}
