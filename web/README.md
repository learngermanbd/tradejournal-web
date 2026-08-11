# TradeJournal Web / PWA

TradeJournal is moving from an Android-first prototype to a responsive web application and installable Progressive Web App. The existing HTML prototypes remain in the repository as product and visual references. Open `preview-all-pages.html` directly to inspect the interactive marketing, user, market, and admin page map without starting the React app.

## Current implementation

The public marketing page is the default home page at `/` and `/marketing.html`. The authenticated React app is available at `/app.html`; its responsive user dashboard includes market, journal, imports, analysis, accounts, diary, reports, settings, ROI-aware trade entry, and major-provider sign-in buttons. Google, Apple, Microsoft, and GitHub use Supabase OAuth; Telegram is supported through a separately configured bot authorization URL. The admin shell is protected behind the app entrypoint and backend role checks. `src/api.ts` defines the typed Worker API client; configure `VITE_API_BASE_URL`, Supabase provider settings, and a real session before enabling cloud writes. Community has been removed from the active site. Private journal data is never treated as browser storage.

Run locally with `npm install`, then `npm run dev`. The public home is `/`; the authenticated app is `/app.html`; the all-pages preview is `/preview-all-pages.html`; the root setup guide is `../journaledge-setup-guide.html`. The preview and root setup guide are local documentation artifacts and are not included in production Vite inputs. Validate with `npm run typecheck` and `npm run build`.

For account providers, configure `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`, enable Google/Apple/Azure/GitHub in Supabase Auth, and set `VITE_TELEGRAM_AUTH_URL` only after deploying a verified Telegram bot authorization endpoint. Never put service-role keys or bot secrets in browser environment variables.

The local Vite development login also shows two demo buttons:

```text
Demo user:  demo.user@journaledge.local / DemoUser123!
Demo admin: demo.admin@journaledge.local / DemoAdmin123!
```

These are in-memory development sessions only, hidden from production builds, and cannot write to Supabase or R2.

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
https://learngermanwith.fun/          marketing website and home page
https://app.learngermanwith.fun/      user application
https://admin.learngermanwith.fun/    protected admin application
```

Only the public marketing website may be indexed by Google. The user app, admin panel, preview, drafts, and account routes use noindex protection and never appear in sitemaps.

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

- `storage`: Cloudflare R2 encrypted vault objects, attachment objects, and server-side schema migrations
- `calculations`: validated Worker-side P&L, risk, reward, R-multiple, and ROI formulas with frontend previews; ROI is net P&L divided by deployed capital (entry value ÷ leverage)
- `auth`: Supabase session handling and role verification
- `sync`: encrypted vault export/import and conflict resolution
- `integrations`: R2, market quote adapter, read-only brokers, billing, AI consent, and monitoring
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
