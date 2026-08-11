# TradeJournal Web / PWA

TradeJournal is moving from an Android-first prototype to a responsive web application and installable Progressive Web App. The existing HTML prototypes remain in the repository as product and visual references.

## Product direction

The web app must work well on phones, tablets, laptops, and large monitors. It should be installable as a PWA and remain useful offline for the journal's private data.

```text
Browser / installed PWA
  ├─ responsive user workspace
  ├─ IndexedDB private journal vault
  ├─ local calculations and CSV imports
  └─ optional encrypted Google Drive backup

Supabase control plane
  ├─ authentication and sessions
  ├─ user preferences and roles
  ├─ subscription metadata
  ├─ sync metadata
  ├─ admin audit records
  └─ aggregate service health
```

## Privacy boundary

IndexedDB stores trades, entries/exits, stop loss, take profit, P&L, notes, psychology, and screenshots locally. The app must encrypt the vault before an optional Drive backup. Supabase must not receive raw journal content by default.

The backend stores only control-plane metadata. Admins can see aggregate operational data after a verified role claim, but cannot browse private trades or journal notes.

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
/admin                    # route is unavailable to normal users
/admin/users
/admin/reports
/admin/service-health
```

## Core implementation boundaries

- `storage`: IndexedDB repositories and schema migrations
- `calculations`: shared P&L, risk, margin, and R-multiple formulas
- `auth`: Supabase session handling and role verification
- `sync`: encrypted vault export/import and conflict resolution
- `integrations`: Drive, read-only brokers, billing, AI consent, and monitoring
- `ui`: responsive components, accessible forms, charts, and localization

## Web-first build phases

1. **Foundation** — TypeScript PWA shell, responsive design system, routing, and service worker.
2. **Private journal** — IndexedDB, trade CRUD, detailed entry form, automatic calculations, accounts, diary, settings, CSV imports, and duplicate detection.
3. **Authentication** — Supabase email login, password recovery, session persistence, and server-verified admin roles.
4. **Cloud controls** — encrypted Google Drive backup, sync metadata, conflict review, and backend admin overview.
5. **Integrations** — read-only broker adapters, subscriptions, notifications, privacy-safe AI, and monitoring.
6. **Release** — accessibility, localization, security review, browser matrix, PWA install testing, Cloudflare Pages deployment, and CI.

## Free-first deployment

- Static/PWA hosting: Cloudflare Pages
- Control plane: Supabase Free tier during development
- Source and CI: GitHub
- Optional encrypted backup: Google Drive application-data folder
- No provider secrets in browser code except public client configuration
- Broker and AI secrets stay in Edge Functions or a server-side worker

## Definition of done

- Offline journal works without network access.
- A normal user sees no admin navigation or admin route content.
- Admin access requires a backend-issued role claim.
- Trade calculations are shared and covered by automated tests.
- Raw private journal content is encrypted before cloud backup.
- CSV import is validated and duplicate-safe.
- The PWA is keyboard accessible, localized, responsive, and installable.
- Production builds use environment variables and never contain secrets.
