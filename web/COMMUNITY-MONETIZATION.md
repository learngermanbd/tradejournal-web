# JournalEdge community monetization

Advertising is enabled only on the public community website:

```text
https://community.learngermanwith.fun/
```

Ads are not shown in:

- The JournalEdge user application
- The admin panel
- Private groups
- Private messages
- User dashboards
- Private coaching workspaces
- Trade journals or psychology screens

## Monetization options

1. **Contextual ads** — ads based on the public page topic, not private user data.
2. **Direct sponsorships** — clearly labeled sponsor placements for public education pages.
3. **Sponsored strategy content** — visibly marked and reviewed before publication.
4. **Affiliate links** — disclosed on the page and managed by authorized admins.
5. **Ad-free premium community** — premium users can hide community advertising.
6. **Community memberships** — optional paid groups, coaching, and mentor workspaces.

## Privacy rules

- Never use trades, P&L, psychology, screenshots, account balances, or broker data for ad targeting.
- Do not sell personal or trading data.
- Use contextual advertising by default.
- Personalized advertising requires separate user consent and regional compliance.
- Do not load third-party ad scripts inside private or authenticated applications.
- Respect consent withdrawal immediately.
- Keep ad identifiers separate from JournalEdge account and journal identifiers.

## Admin controls

Authorized admins can manage:

- Ad provider configuration
- Public page placements
- Ad formats and sizes
- Frequency caps
- Region and language targeting
- Sponsor approval
- Affiliate disclosures
- Community ad-free entitlements
- Revenue and impression reports
- Provider health and policy violations
- Emergency ad disablement

Every monetization change is audit logged. Content admins can manage sponsorship content; billing admins manage paid entitlements; no single role needs unrestricted access.

## Approved placement types

- Public community feed cards
- Public article sponsorship banners
- Public strategy-page native placements
- Newsletter sponsorship blocks
- Community landing-page placements

Avoid intrusive popups, deceptive buttons, auto-play audio, or ads that imitate trading signals. Sponsored content must never imply guaranteed returns or financial advice.

## Technical boundary

```text
Public community page
  → consent manager
  → contextual ad provider or approved sponsor
  → aggregate revenue event

Private app / admin / private community
  → no ad script
  → no ad identifier
  → no advertising request
```

The admin dashboard must report impressions, clicks, revenue, consent rates, and blocked requests only in aggregate.
