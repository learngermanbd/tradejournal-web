# TradeJournal backend

This directory contains the free-first control-plane backend for TradeJournal. It is intentionally separate from the Android app.

## Responsibilities

The backend stores and serves only control-plane data:

- authenticated user profiles and preferences
- role claims and subscription metadata
- Google Drive sync metadata
- broker connection metadata (never raw credentials in the client)
- aggregate service health
- immutable administrator audit events

Trade history, journal notes, psychology entries, screenshots, and raw account history remain on the Android device by default. If the user enables Drive backup, the app encrypts the vault before uploading it to the Drive application-data folder.

## Provider layout

```text
Supabase Auth       identity and sessions
Supabase Postgres   profiles, roles, subscriptions, sync metadata, audit records
Supabase Edge Fn    authenticated admin overview and future webhook boundaries
Cloudflare Worker   broker/AI adapters when a server-side secret is required
Google Drive        optional user-controlled encrypted vault
Firebase            optional FCM/Crashlytics services; no trade payloads
```

## Database setup

1. Create a Supabase project.
2. Apply `supabase/migrations/001_control_plane.sql` with the Supabase SQL editor or CLI.
3. Create the first administrator from a trusted server session:

```sql
insert into public.user_roles (user_id, role)
values ('AUTH_USER_UUID', 'admin')
on conflict (user_id, role) do nothing;
```

Never grant the admin role from the Android client.

## Edge function setup

The `admin-overview` function uses the caller's JWT and checks the `admin` role in Postgres. Deploy it only after setting the Supabase project secrets:

```bash
supabase functions deploy admin-overview
```

The Android app should call the function with the current Supabase access token. Do not ship a service-role key in the APK.

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
