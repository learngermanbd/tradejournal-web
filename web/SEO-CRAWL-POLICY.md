# JournalEdge search indexing and crawl policy

## Public pages that may be indexed

### Marketing website

```text
https://learngermanwith.fun/
```

Indexable content:

- Homepage
- Product pages
- Public pricing
- Public education pages
- Public help articles
- Public legal pages
- Public language pages

### Community website

```text
https://community.learngermanwith.fun/
```

Only intentionally public community content may be indexed:

- Public discussions
- Public strategy pages
- Public education content
- Public coach/mentor profiles when explicitly published
- Public community landing pages

Private groups, messages, user dashboards, saved searches, drafts, and authenticated content must not be indexed.

## Domains that must not be indexed

```text
https://app.learngermanwith.fun/
https://admin.learngermanwith.fun/
```

The user app and admin panel must send:

```text
X-Robots-Tag: noindex, nofollow, noarchive, nosnippet
```

Their authenticated pages should also include:

```html
<meta name="robots" content="noindex, nofollow, noarchive, nosnippet">
```

## Community private routes

The community domain should use `noindex` for:

```text
/login
/account
/settings
/messages
/notifications
/groups/private/*
/drafts/*
/saved/*
/api/*
/search
```

Public community pages need stable canonical URLs, descriptive metadata, Open Graph cards, and inclusion in the public community sitemap.

## Sitemap policy

Publish separate sitemaps:

```text
https://learngermanwith.fun/sitemap.xml
https://community.learngermanwith.fun/sitemap.xml
```

Do not include user-app or admin URLs in any sitemap.

## Security rule

Robots rules are not authentication. Private pages must still require Supabase authentication and Worker authorization. `robots.txt`, `noindex`, CSP, and authentication are separate layers.
