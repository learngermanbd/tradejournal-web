import { ApiError } from './api'

const SUPABASE_URL = (import.meta.env.VITE_SUPABASE_URL as string | undefined)?.replace(/\/$/, '')
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined

export interface AuthSession {
  accessToken: string
  refreshToken: string
  user: { id: string; email?: string; user_metadata?: Record<string, unknown> }
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
