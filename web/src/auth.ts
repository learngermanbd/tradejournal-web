import { ApiError } from './api'

const SUPABASE_URL = (import.meta.env.VITE_SUPABASE_URL as string | undefined)?.replace(/\/$/, '')
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined
const TELEGRAM_AUTH_URL = import.meta.env.VITE_TELEGRAM_AUTH_URL as string | undefined

export type OAuthProvider = 'google' | 'apple' | 'azure' | 'github' | 'telegram'

export interface AuthSession {
  accessToken: string
  refreshToken: string
  user: { id: string; email?: string; phone?: string; user_metadata?: Record<string, unknown> }
}

export async function signIn(email: string, password: string): Promise<AuthSession> {
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY) throw new Error('Supabase environment variables are not configured.')
  const response = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: { apikey: SUPABASE_ANON_KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const body = await response.json().catch(() => ({})) as { access_token?: string; refresh_token?: string; user?: AuthSession['user']; error_description?: string; msg?: string }
  if (!response.ok || !body.access_token || !body.refresh_token || !body.user) throw new ApiError(response.status, body.error_description ?? body.msg ?? 'Unable to sign in.')
  return { accessToken: body.access_token, refreshToken: body.refresh_token, user: body.user }
}

export async function signUp(email: string, password: string): Promise<{ needsConfirmation: boolean }> {
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY) throw new Error('Supabase environment variables are not configured.')
  const response = await fetch(`${SUPABASE_URL}/auth/v1/signup`, {
    method: 'POST',
    headers: { apikey: SUPABASE_ANON_KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const body = await response.json().catch(() => ({})) as { access_token?: string; error_description?: string; msg?: string }
  if (!response.ok) throw new ApiError(response.status, body.error_description ?? body.msg ?? 'Unable to create account.')
  return { needsConfirmation: !body.access_token }
}

export function startOAuth(provider: OAuthProvider): never {
  if (provider === 'telegram') {
    if (!TELEGRAM_AUTH_URL) throw new Error('Telegram sign-in needs a configured Telegram bot authorization URL.')
    window.location.assign(TELEGRAM_AUTH_URL)
    throw new Error('Redirecting to Telegram…')
  }
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY) throw new Error('Supabase environment variables are not configured.')
  const redirectTo = `${window.location.origin}/app.html`
  const params = new URLSearchParams({ provider, redirect_to: redirectTo, apikey: SUPABASE_ANON_KEY })
  window.location.assign(`${SUPABASE_URL}/auth/v1/authorize?${params.toString()}`)
  throw new Error('Redirecting to sign-in provider…')
}

export function getOAuthSession(): AuthSession | null {
  const hash = window.location.hash.replace(/^#/, '')
  if (!hash) return null
  const params = new URLSearchParams(hash)
  const accessToken = params.get('access_token')
  const refreshToken = params.get('refresh_token')
  if (!accessToken || !refreshToken) return null
  window.history.replaceState({}, document.title, `${window.location.pathname}${window.location.search}`)
  return {
    accessToken,
    refreshToken,
    user: { id: 'oauth-session', email: params.get('email') ?? undefined },
  }
}
