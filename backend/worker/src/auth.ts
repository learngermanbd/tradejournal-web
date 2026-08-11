export interface AuthUser {
  id: string
  email?: string
}

export interface AuthContext {
  user: AuthUser
  token: string
}

export interface AuthEnvironment {
  SUPABASE_URL: string
  SUPABASE_ANON_KEY: string
}

export async function authenticate(request: Request, env: AuthEnvironment): Promise<AuthContext | Response> {
  const header = request.headers.get('Authorization')
  if (!header?.startsWith('Bearer ')) return json({ error: 'Authentication required' }, 401)
  const token = header.slice('Bearer '.length).trim()
  if (!token) return json({ error: 'Authentication required' }, 401)

  const response = await fetch(`${env.SUPABASE_URL}/auth/v1/user`, {
    headers: { apikey: env.SUPABASE_ANON_KEY, Authorization: `Bearer ${token}` },
  })
  if (!response.ok) return json({ error: 'Invalid session' }, 401)
  const user = await response.json() as AuthUser
  return { user, token }
}

export async function isAdmin(context: AuthContext, env: AuthEnvironment): Promise<boolean> {
  const response = await fetch(`${env.SUPABASE_URL}/rest/v1/user_roles?user_id=eq.${encodeURIComponent(context.user.id)}&role=eq.admin&select=user_id`, {
    headers: { apikey: env.SUPABASE_ANON_KEY, Authorization: `Bearer ${context.token}` },
  })
  if (!response.ok) return false
  const rows = await response.json() as Array<{ user_id: string }>
  return rows.length > 0
}

export function json(body: unknown, status = 200, headers: HeadersInit = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8', ...headers },
  })
}
