# TradeJournal Web / PWA

TradeJournal is moving from an Android-first prototype to a responsive web application and installable Progressive Web App. The existing HTML prototypes remain in the repository as product and visual references.

## Product direction

The web app must work well on phones, tablets, laptops, and large monitors. It should be installable as a PWA and remain reachable online, while only caching the app shell and temporary request state in the browser.

```text
Browser / installed PWA
  ├─ responsive user workspace
  ├─ in-memory forms and short-lived cache only
  ├─ cloud calculations and CSV processing
  └─ encrypted cloud journal storage

Supabase control plane
  ├─ authentication and sessions
  ├─ user preferences and roles
  ├─ subscription metadata
  ├─ sync metadata
  ├─ admin audit records
  └─ aggregate service health
```

## Privacy boundary

Saved trades, entries/exits, stop loss, take profit, P&L, notes, psychology, and screenshots go to cloud services. Private journal chunks and attachments are encrypted before Cloudflare R2 upload. Supabase stores control-plane data; it must not receive raw private journal content by default.

The backend stores only control-plane metadata. Admins can see aggregate operational data after a verified role claim, but cannot browse private trades or journal notes.

## Public domain layout

```text
https://learngermanwith.fun/          marketing website
https://app.learngermanwith.fun/      user application
https://community.learngermanwith.fun/ community application
https://admin.learngermanwith.fun/    protected admin application
```

The community application shares authentication and branding with the user app but has separate navigation, moderation controls, sharing permissions, community data boundaries, and public-only monetization. See `SEO-CRAWL-POLICY.md` and `COMMUNITY-MONETIZATION.md` for indexing and advertising rules.

Only the marketing website and intentionally published community content may be indexed by Google. The user app, admin panel, authenticated community pages, private groups, messages, drafts, and account routes must use noindex protection and never appear in sitemaps.

## Planned application routes

```text
/login
/dashboard
/journal
/journal/new
/imports
/analysis
/accounts
/diary
/settings
/community                # separate community domain/application
/admin                    # route is unavailable to normal users
/admin/users
/admin/reports
/admin/service-health
```

## Core implementation boundaries

- `storage`: Cloudflare R2 vault chunks, attachment objects, and server-side schema migrations
- `calculations`: shared P&L, risk, margin, and R-multiple formulas
- `auth`: Supabase session handling and role verification
- `sync`: encrypted vault export/import and conflict resolution
- `integrations`: R2, read-only brokers, billing, AI consent, and monitoring
- `ui`: responsive components, accessible forms, charts, and localization
- `monetization`: contextual ads, sponsorships, affiliate disclosures, memberships, and ad-free entitlements

## Web-first build phases

1. **Foundation** — TypeScript PWA shell, responsive design system, routing, service worker, and SaaS dashboard shell.
2. **Cloud journal** — Worker API, encrypted R2 vault chunks, trade CRUD, calculations, accounts, diary, settings, CSV imports, and duplicate detection.
3. **Authentication** — Supabase email login, password recovery, session persistence, MFA, and server-verified admin roles.
4. **Cloud controls** — R2 versioning, encrypted journal chunks, sync metadata, conflict review, and backend admin overview.
5. **Integrations** — read-only broker adapters, subscriptions, notifications, privacy-safe AI, and monitoring.
6. **Release** — accessibility, localization, security review, browser matrix, PWA install testing, Cloudflare Pages deployment, and CI.

## Free-first deployment

- Static/PWA hosting: Cloudflare Pages
- Control plane: Supabase Free tier during development
- Source and CI: GitHub
- Encrypted journal storage: Cloudflare R2
- No provider secrets in browser code except public client configuration
- Broker and AI secrets stay in Edge Functions or a server-side worker

## Definition of done

- The PWA shell loads quickly and queues safe requests when connectivity briefly drops; saved data remains cloud-backed.
- A normal user sees no admin navigation or admin route content.
- Admin access requires a backend-issued role claim.
- Trade calculations are shared and covered by automated tests.
- Raw private journal content is encrypted before cloud backup.
- CSV import is validated and duplicate-safe.
- The PWA is keyboard accessible, localized, responsive, and installable.
- Production builds use environment variables and never contain secrets.
