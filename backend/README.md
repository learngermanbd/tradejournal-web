# JournalEdge backend

This directory contains the free-first backend for the JournalEdge web/PWA. It is intentionally separate from the browser client.

## Implemented API foundation

`worker/` contains a Cloudflare Worker API with:

- authenticated Supabase session checks
- encrypted R2 trade vault reads and writes
- server-side trade validation and P&L/risk/ROI calculations

ROI is calculated as `net P&L / deployed capital × 100`. Deployed capital is `entry price × quantity / leverage`; when leverage is omitted, it defaults to 1x.
- delete-by-owner trade operations
- server-side profile settings updates
- protected aggregate admin metrics
- a market quote adapter with a manual fallback
- CORS allowlisting and security headers

The frontend calls these routes through `VITE_API_BASE_URL`. Configure Supabase and R2 secrets before enabling production writes.

## Responsibilities

The backend stores and serves only control-plane data:

- authenticated user profiles and preferences
- role claims and subscription metadata
- Cloudflare R2 sync metadata
- broker connection metadata (never raw credentials in the client)
- aggregate service health
- immutable administrator audit events

Trade history, journal notes, psychology entries, screenshots, and raw account history are stored as encrypted Cloudflare R2 vault chunks. The browser is not the permanent data store.

## Provider layout

```text
Supabase Auth       identity and sessions
Supabase Postgres   profiles, roles, subscriptions, sync metadata, audit records
Supabase Edge Fn    authenticated admin overview and future webhook boundaries
Cloudflare Worker   broker/AI adapters when a server-side secret is required
Cloudflare R2       encrypted journal vault and attachments
Firebase            optional FCM/Crashlytics services; no trade payloads
```

## Database setup

1. Create a Supabase project.
2. Apply `supabase/migrations/001_control_plane.sql` and `supabase/migrations/002_product_control_plane.sql` with the Supabase SQL editor or CLI.
3. Create the first administrator from a trusted server session:

```sql
insert into public.user_roles (user_id, role)
values ('AUTH_USER_UUID', 'admin')
on conflict (user_id, role) do nothing;
```

Never grant the admin role from browser code.

## Edge function setup

The `admin-overview` function uses the caller's JWT and checks the `admin` role in Postgres. Deploy it only after setting the Supabase project secrets:

```bash
supabase functions deploy admin-overview
```

The web client should call the function with the current Supabase access token. Do not ship a service-role key in browser code.

## Worker development

```bash
cd backend/worker
npm install
npm run typecheck
npm test
npx wrangler dev
```

Set the required secrets before deployment:

```bash
npx wrangler secret put SUPABASE_URL
npx wrangler secret put SUPABASE_ANON_KEY
npx wrangler secret put VAULT_ENCRYPTION_KEY
```

`VAULT_ENCRYPTION_KEY` must be a base64-encoded 32-byte AES key. R2 stores encrypted trade vault objects under a user-scoped key; the browser never receives the encryption secret.

## Environment variables

Use the deployment platform's secret store. Do not commit real values.

```text
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_ANON_KEY
```

The anon key is safe to distribute with RLS enabled. The service-role key is server-only.

## Security rules

- RLS is enabled on every public table.
- Users can access only their own profile, subscription, and sync metadata.
- Administrators can access operational metadata and aggregate control-plane records.
- Administrators cannot query private trade content because it is not stored in this database.
- Admin mutations must create an audit event.
- Broker tokens and AI provider keys must stay in server-side secret storage.
- Every external adapter must be read-only until a separate, explicit write-scope review is approved.
