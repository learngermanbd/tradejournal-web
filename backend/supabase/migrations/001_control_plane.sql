-- TradeJournal control plane. Apply with the Supabase SQL editor or Supabase CLI.
-- Private trade content is intentionally not represented in this schema.

create extension if not exists pgcrypto;

do $$
begin
  create type public.app_role as enum ('user', 'admin');
exception
  when duplicate_object then null;
end $$;

do $$
begin
  create type public.subscription_plan as enum ('free', 'premium');
exception
  when duplicate_object then null;
end $$;

do $$
begin
  create type public.subscription_status as enum ('inactive', 'trialing', 'active', 'past_due', 'cancelled');
exception
  when duplicate_object then null;
end $$;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  display_name text,
  locale text not null default 'en',
  currency text not null default 'USD',
  experience_level text not null default 'beginner',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.user_roles (
  user_id uuid not null references auth.users(id) on delete cascade,
  role public.app_role not null default 'user',
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, role)
);

create table if not exists public.subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null unique references auth.users(id) on delete cascade,
  plan public.subscription_plan not null default 'free',
  status public.subscription_status not null default 'inactive',
  provider text,
  provider_customer_id text,
  current_period_end timestamptz,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.sync_metadata (
  user_id uuid primary key references auth.users(id) on delete cascade,
  provider text not null default 'cloudflare_r2',
  status text not null default 'never_synced',
  last_sync_at timestamptz,
  remote_version text,
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.admin_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_user_id uuid not null references auth.users(id),
  action text not null,
  target_user_id uuid references auth.users(id),
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.service_events (
  id uuid primary key default gen_random_uuid(),
  service_name text not null,
  status text not null,
  latency_ms integer,
  error_code text,
  created_at timestamptz not null default timezone('utc', now())
);

create index if not exists user_roles_user_id_idx on public.user_roles(user_id);
create index if not exists audit_actor_created_idx on public.admin_audit_log(actor_user_id, created_at desc);
create index if not exists service_events_created_idx on public.service_events(service_name, created_at desc);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = public
as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists subscriptions_set_updated_at on public.subscriptions;
create trigger subscriptions_set_updated_at
before update on public.subscriptions
for each row execute function public.set_updated_at();

drop trigger if exists sync_metadata_set_updated_at on public.sync_metadata;
create trigger sync_metadata_set_updated_at
before update on public.sync_metadata
for each row execute function public.set_updated_at();

create or replace function public.has_role(check_user_id uuid, required_role public.app_role)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.user_roles
    where user_id = check_user_id
      and role = required_role
  );
$$;

revoke all on function public.has_role(uuid, public.app_role) from public;
grant execute on function public.has_role(uuid, public.app_role) to authenticated;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, display_name)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name')
  )
  on conflict (id) do update set email = excluded.email;

  insert into public.user_roles (user_id, role)
  values (new.id, 'user')
  on conflict (user_id, role) do nothing;

  insert into public.subscriptions (user_id)
  values (new.id)
  on conflict (user_id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.user_roles enable row level security;
alter table public.subscriptions enable row level security;
alter table public.sync_metadata enable row level security;
alter table public.admin_audit_log enable row level security;
alter table public.service_events enable row level security;

drop policy if exists profiles_select_self_or_admin on public.profiles;
create policy profiles_select_self_or_admin on public.profiles
for select to authenticated
using (id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));

drop policy if exists profiles_update_self on public.profiles;
create policy profiles_update_self on public.profiles
for update to authenticated
using (id = (select auth.uid()))
with check (id = (select auth.uid()));

drop policy if exists roles_select_self_or_admin on public.user_roles;
create policy roles_select_self_or_admin on public.user_roles
for select to authenticated
using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));

drop policy if exists subscriptions_select_self_or_admin on public.subscriptions;
create policy subscriptions_select_self_or_admin on public.subscriptions
for select to authenticated
using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));

drop policy if exists sync_select_self_or_admin on public.sync_metadata;
create policy sync_select_self_or_admin on public.sync_metadata
for select to authenticated
using (user_id = (select auth.uid()) or public.has_role((select auth.uid()), 'admin'));

drop policy if exists sync_insert_self on public.sync_metadata;
create policy sync_insert_self on public.sync_metadata
for insert to authenticated
with check (user_id = (select auth.uid()));

drop policy if exists sync_update_self on public.sync_metadata;
create policy sync_update_self on public.sync_metadata
for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

drop policy if exists audit_select_admin on public.admin_audit_log;
create policy audit_select_admin on public.admin_audit_log
for select to authenticated
using (public.has_role((select auth.uid()), 'admin'));

drop policy if exists service_events_select_admin on public.service_events;
create policy service_events_select_admin on public.service_events
for select to authenticated
using (public.has_role((select auth.uid()), 'admin'));

-- No client INSERT/UPDATE/DELETE policies exist for roles, subscriptions,
-- audit logs, or service events. Those mutations require trusted server code.
