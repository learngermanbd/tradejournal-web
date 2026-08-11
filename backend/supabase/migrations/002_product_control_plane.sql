-- JournalEdge product controls. Private trades and journal content remain in encrypted R2 vaults.

create table if not exists public.feature_flags (
  key text primary key,
  enabled boolean not null default false,
  minimum_plan public.subscription_plan not null default 'free',
  rollout_percent integer not null default 100 check (rollout_percent between 0 and 100),
  updated_by uuid references auth.users(id),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.broker_connections (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  broker_name text not null,
  account_label text not null,
  status text not null default 'disconnected',
  last_sync_at timestamptz,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.community_posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text not null,
  visibility text not null default 'private' check (visibility in ('private', 'group', 'public')),
  status text not null default 'draft' check (status in ('draft', 'published', 'archived')),
  allow_indexing boolean not null default false,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists broker_connections_user_idx on public.broker_connections(user_id);
create index if not exists community_posts_public_idx on public.community_posts(status, visibility, allow_indexing);

 drop trigger if exists broker_connections_set_updated_at on public.broker_connections;
 create trigger broker_connections_set_updated_at
 before update on public.broker_connections
 for each row execute function public.set_updated_at();

 drop trigger if exists community_posts_set_updated_at on public.community_posts;
 create trigger community_posts_set_updated_at
 before update on public.community_posts
 for each row execute function public.set_updated_at();

alter table public.feature_flags enable row level security;
alter table public.broker_connections enable row level security;
alter table public.community_posts enable row level security;

create policy feature_flags_select_authenticated on public.feature_flags
for select to authenticated using (true);

create policy broker_connections_self on public.broker_connections
for select to authenticated using (user_id = (select auth.uid()));

create policy community_posts_public_or_owner on public.community_posts
for select to anon, authenticated
using ((status = 'published' and visibility = 'public' and allow_indexing = true) or author_id = (select auth.uid()));

create policy community_posts_insert_self on public.community_posts
for insert to authenticated
with check (author_id = (select auth.uid()));

create policy community_posts_update_self on public.community_posts
for update to authenticated
using (author_id = (select auth.uid()))
with check (author_id = (select auth.uid()));

-- Feature mutations, broker credentials, moderation, and audit records remain server-only.
