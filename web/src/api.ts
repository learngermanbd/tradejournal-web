import type { Trade, TradeInput } from './trade'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? ''

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly fields?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
  }
}

export interface Quote {
  symbol: string
  price: number | null
  status: 'manual' | 'provider'
  message?: string
  quote?: unknown
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, accessToken?: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(init.headers ?? {}),
    },
  })
  const body = await response.json().catch(() => ({})) as { error?: string; fields?: Record<string, string> } & T
  if (!response.ok) throw new ApiError(response.status, body.error ?? 'Request failed', body.fields)
  return body
}

export function listTrades(accessToken: string) {
  return apiRequest<{ trades: Trade[] }>('/api/trades', { method: 'GET' }, accessToken)
}

export function saveTrade(input: TradeInput, accessToken: string) {
  return apiRequest<{ trade: Trade }>('/api/trades', { method: 'POST', body: JSON.stringify(input) }, accessToken)
}

export function deleteTrade(id: string, accessToken: string) {
  return apiRequest<{ deleted: string }>(`/api/trades/${encodeURIComponent(id)}`, { method: 'DELETE' }, accessToken)
}

export function getQuote(symbol: string) {
  return apiRequest<Quote>(`/api/market/quote?symbol=${encodeURIComponent(symbol)}`)
}
