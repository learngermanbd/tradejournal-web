import { authenticate, isAdmin, json, type AuthContext } from './auth'
import { createTrade, validateTrade, type Trade, type TradeInput } from './domain'
import { EncryptedTradeStore, type VaultBucket } from './storage'

export interface Env {
  JOURNAL_VAULT: VaultBucket
  SUPABASE_URL: string
  SUPABASE_ANON_KEY: string
  VAULT_ENCRYPTION_KEY: string
  ALLOWED_ORIGINS?: string
  MARKET_DATA_URL?: string
  MARKET_DATA_API_KEY?: string
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const origin = request.headers.get('Origin')
    const cors = corsHeaders(origin, env.ALLOWED_ORIGINS)
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors })

    try {
      const url = new URL(request.url)
      const response = await route(request, env, url)
      const headers = new Headers(response.headers)
      Object.entries(cors).forEach(([key, value]) => headers.set(key, value))
      headers.set('X-Content-Type-Options', 'nosniff')
      headers.set('Referrer-Policy', 'same-origin')
      headers.set('X-Frame-Options', 'DENY')
      return new Response(response.body, { status: response.status, headers })
    } catch (error) {
      console.error('Unhandled API error', error)
      return json({ error: 'Internal server error' }, 500, cors)
    }
  },
}

async function route(request: Request, env: Env, url: URL): Promise<Response> {
  if (request.method === 'GET' && url.pathname === '/health') return json({ status: 'ok', service: 'journaledge-api', time: new Date().toISOString() })

  if (url.pathname === '/api/market/quote' && request.method === 'GET') return marketQuote(url, env)

  const context = await authenticate(request, env)
  if (context instanceof Response) return context

  if (url.pathname === '/api/session' && request.method === 'GET') return json({ user: context.user })
  if (url.pathname === '/api/trades' && request.method === 'GET') return listTrades(context, env)
  if (url.pathname === '/api/trades' && request.method === 'POST') return addTrade(request, context, env)
  if (url.pathname.startsWith('/api/trades/') && request.method === 'DELETE') return deleteTrade(url, context, env)
  if (url.pathname === '/api/settings' && request.method === 'GET') return getSettings(context, env)
  if (url.pathname === '/api/settings' && request.method === 'PATCH') return updateSettings(request, context, env)
  if (url.pathname === '/api/admin/overview' && request.method === 'GET') return adminOverview(context, env)

  return json({ error: 'Not found' }, 404)
}

async function listTrades(context: AuthContext, env: Env) {
  const store = new EncryptedTradeStore(env.JOURNAL_VAULT, env.VAULT_ENCRYPTION_KEY)
  return json({ trades: await store.getTrades(context.user.id) })
}

async function addTrade(request: Request, context: AuthContext, env: Env) {
  const input = await parseJson<TradeInput>(request)
  const validation = validateTrade(input)
  if (!validation.valid) return json({ error: 'Validation failed', fields: validation.errors }, 422)
  const trade = createTrade(input)
  const store = new EncryptedTradeStore(env.JOURNAL_VAULT, env.VAULT_ENCRYPTION_KEY)
  const trades = await store.getTrades(context.user.id)
  trades.unshift(trade)
  await store.saveTrades(context.user.id, trades)
  return json({ trade }, 201)
}

async function deleteTrade(url: URL, context: AuthContext, env: Env) {
  const id = url.pathname.slice('/api/trades/'.length)
  if (!id) return json({ error: 'Trade id is required' }, 400)
  const store = new EncryptedTradeStore(env.JOURNAL_VAULT, env.VAULT_ENCRYPTION_KEY)
  const trades = await store.getTrades(context.user.id)
  const next = trades.filter((trade) => trade.id !== id)
  if (next.length === trades.length) return json({ error: 'Trade not found' }, 404)
  await store.saveTrades(context.user.id, next)
  return json({ deleted: id })
}

async function getSettings(context: AuthContext, env: Env) {
  const response = await supabaseRequest(`/rest/v1/profiles?id=eq.${encodeURIComponent(context.user.id)}&select=id,display_name,locale,currency,experience_level`, context, env)
  if (!response.ok) return json({ error: 'Unable to load settings' }, 502)
  const rows = await response.json()
  return json({ settings: Array.isArray(rows) ? rows[0] ?? null : null })
}

async function updateSettings(request: Request, context: AuthContext, env: Env) {
  const body = await parseJson<Record<string, unknown>>(request)
  const allowed = ['display_name', 'locale', 'currency', 'experience_level']
  const updates = Object.fromEntries(Object.entries(body).filter(([key, value]) => allowed.includes(key) && typeof value === 'string'))
  if (Object.keys(updates).length === 0) return json({ error: 'No valid settings supplied' }, 422)
  const response = await supabaseRequest(`/rest/v1/profiles?id=eq.${encodeURIComponent(context.user.id)}`, context, env, { method: 'PATCH', body: JSON.stringify(updates), headers: { Prefer: 'return=representation' } })
  if (!response.ok) return json({ error: 'Unable to save settings' }, 502)
  return json({ settings: (await response.json() as unknown[])[0] ?? null })
}

async function adminOverview(context: AuthContext, env: Env) {
  if (!(await isAdmin(context, env))) return json({ error: 'Admin role required' }, 403)
  const [users, subscriptions, events] = await Promise.all([
    supabaseRequest('/rest/v1/profiles?select=id', context, env, { headers: { Prefer: 'count=exact', Range: '0-0' } }),
    supabaseRequest('/rest/v1/subscriptions?status=eq.active&select=id', context, env, { headers: { Prefer: 'count=exact', Range: '0-0' } }),
    supabaseRequest('/rest/v1/service_events?select=service_name,status,created_at&order=created_at.desc&limit=20', context, env),
  ])
  if (!users.ok || !subscriptions.ok || !events.ok) return json({ error: 'Unable to load admin metrics' }, 502)
  return json({
    generatedAt: new Date().toISOString(),
    activeUsers: parseContentRange(users.headers.get('Content-Range')),
    activeSubscriptions: parseContentRange(subscriptions.headers.get('Content-Range')),
    recentServiceEvents: await events.json(),
    privacy: { rawTradesIncluded: false, journalNotesIncluded: false, psychologyIncluded: false },
  })
}

async function marketQuote(url: URL, env: Env) {
  const symbol = url.searchParams.get('symbol')?.trim().toUpperCase()
  if (!symbol || !/^[A-Z0-9._/-]{1,20}$/.test(symbol)) return json({ error: 'A valid symbol is required' }, 400)
  if (!env.MARKET_DATA_URL) return json({ symbol, price: null, status: 'manual', message: 'Configure a server-side market provider to enable quotes.' })
  const providerUrl = new URL(env.MARKET_DATA_URL)
  providerUrl.searchParams.set('symbol', symbol)
  if (env.MARKET_DATA_API_KEY) providerUrl.searchParams.set('apikey', env.MARKET_DATA_API_KEY)
  const response = await fetch(providerUrl)
  if (!response.ok) return json({ error: 'Market provider unavailable' }, 502)
  return json({ symbol, status: 'provider', quote: await response.json() })
}

async function supabaseRequest(path: string, context: AuthContext, env: Env, init: RequestInit = {}) {
  return fetch(`${env.SUPABASE_URL}${path}`, {
    ...init,
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      Authorization: `Bearer ${context.token}`,
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    },
  })
}

async function parseJson<T>(request: Request): Promise<T> {
  try {
    return await request.json() as T
  } catch {
    throw new Error('Invalid JSON body')
  }
}

function parseContentRange(value: string | null) {
  const match = value?.match(/\/([0-9]+)$/)
  return match ? Number(match[1]) : 0
}

function corsHeaders(origin: string | null, configured: string | undefined): Record<string, string> {
  const allowed = (configured ?? '').split(',').map((item) => item.trim()).filter(Boolean)
  const allowOrigin = origin && allowed.includes(origin) ? origin : allowed[0] ?? 'https://app.learngermanwith.fun'
  return {
    'Access-Control-Allow-Origin': allowOrigin,
    'Access-Control-Allow-Methods': 'GET,POST,PATCH,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization, Content-Type',
    Vary: 'Origin',
  }
}
