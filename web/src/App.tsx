import { useMemo, useState } from 'react'
import { adminMetrics, userMetrics } from './dashboardData'
import { ApiError, getQuote, saveTrade } from './api'
import { getOAuthSession, signIn, signUp, startOAuth, type AuthSession, type OAuthProvider } from './auth'
import { previewTrade, type TradeInput, type TradeMarket, type TradeSide } from './trade'

type Area = 'user' | 'admin'
type UserView = 'overview' | 'journal' | 'imports' | 'analysis' | 'market' | 'accounts' | 'diary' | 'reports' | 'settings'
type AdminView = 'overview' | 'users' | 'features' | 'content' | 'integrations' | 'billing' | 'operations' | 'audit'

const userNav: Array<[UserView, string, string]> = [
  ['overview', 'Overview', '⌂'],
  ['journal', 'Journal', '▤'],
  ['imports', 'Imports', '⇩'],
  ['analysis', 'Analysis', '◔'],
  ['market', 'Market', '⌁'],
  ['accounts', 'Accounts', '◎'],
  ['diary', 'Diary & goals', '✎'],
  ['reports', 'Reports', '▦'],
  ['settings', 'Settings', '⚙'],
]

const adminNav: Array<[AdminView, string, string]> = [
  ['overview', 'Overview', '⌂'],
  ['users', 'Users', '♙'],
  ['features', 'Features', '◆'],
  ['content', 'Content CMS', '▣'],
  ['integrations', 'Providers', '⌁'],
  ['billing', 'Billing', '$'],
  ['operations', 'Operations', '▥'],
  ['audit', 'Audit & security', '≡'],
]

function App() {
  const initialArea = getAreaFromPath()
  const [area, setArea] = useState<Area>(initialArea)
  const [userView, setUserView] = useState<UserView>('overview')
  const [adminView, setAdminView] = useState<AdminView>('overview')
  const [dark, setDark] = useState(false)
  const [showTradeForm, setShowTradeForm] = useState(false)
  const [session, setSession] = useState<AuthSession | null>(() => getOAuthSession())

  if (getRoute() === 'login' && !session) return <LoginScreen onSignedIn={setSession} />

  const title = useMemo(() => area === 'admin' ? 'Admin control center' : 'Good morning, Alex', [area])

  function changeArea(next: Area) {
    setArea(next)
    if (next === 'admin') setAdminView('overview')
    if (next === 'user') setUserView('overview')
  }

  return (
    <div className={dark ? 'app dark' : 'app'}>
      <aside className="sidebar">
        <Brand />
        <div className="area-switcher" aria-label="Application area">
          <button className={area === 'user' ? 'area active' : 'area'} onClick={() => changeArea('user')}>User app</button>
          {area === 'admin' && <button className="area active" onClick={() => changeArea('admin')}>Admin</button>}
        </div>
        <nav className="nav" aria-label="Primary navigation">
          <span className="nav-label">{area === 'admin' ? 'CONTROL CENTER' : 'WORKSPACE'}</span>
          {(area === 'admin' ? adminNav : userNav).map(([key, label, icon]) => {
            const selected = area === 'admin' ? adminView === key : userView === key
            return <button key={key} className={selected ? 'nav-item active' : 'nav-item'} onClick={() => selectView(area, key, setUserView, setAdminView)}><span>{icon}</span>{label}</button>
          })}
          {area !== 'admin' && <>
            <span className="nav-label improve">IMPROVE</span>
            <button className="nav-item" onClick={() => setUserView('journal')}><span>✎</span>Trading diary</button>
          </>}
        </nav>
        <div className="sidebar-bottom">
          <div className="privacy-note"><strong>Cloud-first & encrypted</strong><span>Saved data is protected by Worker API and R2 vaults.</span></div>
          <div className="account-chip"><span className="avatar">{session ? (session.user.email?.slice(0, 2).toUpperCase() ?? 'AK') : 'AK'}</span><span><strong>{session?.user.email ?? 'Alex Kim'}</strong><small>{area === 'admin' ? 'Super admin' : session ? 'Cloud session active' : 'Demo workspace'}</small></span></div>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="heading"><span className="eyebrow">JournalEdge SaaS</span><h1>{title}</h1></div>
          <div className="top-actions"><span className="domain-pill">{area === 'admin' ? 'admin.' : 'app.'}learngermanwith.fun</span><button className="icon-button" aria-label="Toggle theme" onClick={() => setDark(!dark)}>{dark ? '☀' : '◐'}</button><button className="icon-button" aria-label="Notifications">♢</button>{area === 'user' && <button className="primary-button" onClick={() => setShowTradeForm(true)}>＋ Add trade</button>}</div>
        </header>

        {area === 'admin' ? <AdminDashboard view={adminView} /> : <UserDashboard view={userView} onAddTrade={() => setShowTradeForm(true)} />}
      </main>
      {showTradeForm && <TradeForm accessToken={session?.accessToken} onClose={() => setShowTradeForm(false)} onSaved={() => setShowTradeForm(false)} />}
    </div>
  )
}

function LoginScreen({ onSignedIn }: { onSignedIn: (session: AuthSession) => void }) {
  const [mode, setMode] = useState<'signin' | 'signup'>('signin')
  const [providerMessage, setProviderMessage] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  function handleOAuth(provider: OAuthProvider) {
    try {
      startOAuth(provider)
    } catch (error) {
      setProviderMessage(error instanceof Error ? error.message : 'Unable to start provider sign-in.')
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setMessage('')
    try {
      if (mode === 'signin') onSignedIn(await signIn(email, password))
      else {
        const result = await signUp(email, password)
        setMessage(result.needsConfirmation ? 'Check your email to confirm your account, then sign in.' : 'Account created. You can now sign in.')
        setMode('signin')
      }
    } catch (error) {
      setMessage(error instanceof ApiError || error instanceof Error ? error.message : 'Authentication failed.')
    } finally {
      setBusy(false)
    }
  }

  return <main className="auth-page"><div className="auth-card"><Brand /><span className="eyebrow">Secure cloud access</span><h1>{mode === 'signin' ? 'Welcome back' : 'Create your journal'}</h1><p>Trade records are stored in your encrypted cloud vault, not in browser storage.</p><div className="social-grid"><button className="social-button" onClick={() => handleOAuth('google')}><b>G</b> Google</button><button className="social-button" onClick={() => handleOAuth('apple')}><b>●</b> Apple</button><button className="social-button" onClick={() => handleOAuth('azure')}><b>▦</b> Microsoft</button><button className="social-button" onClick={() => handleOAuth('github')}><b>◆</b> GitHub</button><button className="social-button telegram" onClick={() => handleOAuth('telegram')}><b>➤</b> Telegram</button></div>{providerMessage && <div className="form-status provider-status">{providerMessage}</div>}<div className="auth-divider"><span>or use email</span></div><form onSubmit={submit}><label>Email<input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label><label>Password<input type="password" required minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" /></label>{message && <div className="form-status">{message}</div>}<button className="primary-button auth-submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'signin' ? 'Sign in securely' : 'Create account'}</button></form><button className="auth-switch" onClick={() => setMode(mode === 'signin' ? 'signup' : 'signin')}>{mode === 'signin' ? 'Need an account? Create one' : 'Already registered? Sign in'}</button><small className="provider-note">Google, Apple, Microsoft, and GitHub use Supabase OAuth. Telegram needs a verified bot authorization URL configured in the Worker environment.</small></div></main>
}

function Brand() {
  return <div className="brand"><div className="brand-logo">↗</div><div><strong>JournalEdge</strong><span>Journal every trade. Sharpen your edge.</span></div></div>
}

function UserDashboard({ view, onAddTrade }: { view: UserView; onAddTrade: () => void }) {
  if (view === 'journal') return <Workspace title="Journal" description="Record every decision with cloud-saved context." action="＋ New trade" onAction={onAddTrade}><DataTable rows={['NVDA · Long · Breakout', 'EUR/USD · Short · Reversal', 'BTC/USDT · Long · Momentum', 'ESM6 · Short · Failed break']} /></Workspace>
  if (view === 'imports') return <Workspace title="Imports" description="Preview, map, reconcile, and approve external records." action="＋ Import CSV"><DataTable rows={['Interactive Brokers · 42 rows · 4 warnings', 'Binance · 18 rows · Ready to approve', 'Manual template · Saved privately']} /></Workspace>
  if (view === 'analysis') return <Workspace title="Analysis" description="Turn cloud-saved records into an honest review." action="March 2026"><BarChart title="Strategy performance" rows={['Breakout · +$890 · 66%', 'Reversal · +$210 · 42%', 'Momentum · +$145 · 50%', 'Support · +$108 · 60%']} /></Workspace>
  if (view === 'market') return <MarketDashboard />
  if (view === 'accounts') return <Workspace title="Accounts" description="Manage live, paper, demo, and prop-firm accounts." action="＋ Connect account"><DataTable rows={['Main account · Interactive Brokers · Live', 'Crypto account · Binance · Live', 'Practice account · Manual · Paper']} /></Workspace>
  if (view === 'diary') return <Workspace title="Diary & goals" description="Track plans, psychology, habits, review streaks, and process goals." action="＋ New reflection"><MetricGrid metrics={[{ label: 'Review streak', value: '12 days', detail: 'Best: 18 days', tone: 'positive' }, { label: 'Plan adherence', value: '78%', detail: '+9% this month', tone: 'blue' }, { label: 'Goals', value: '4/6', detail: 'On track', tone: 'positive' }, { label: 'Private notes', value: '28', detail: 'Encrypted in R2', tone: 'warning' }]} /></Workspace>
  if (view === 'reports') return <Workspace title="Reports & exports" description="Generate performance, tax, coach, PDF, CSV, Excel, and JSON reports." action="＋ Generate report"><DataTable rows={['March performance · PDF · Private', 'Tax-lot export 2026 · CSV · FIFO', 'Coach review · Watermarked · Expires in 14 days']} /></Workspace>
  if (view === 'settings') return <Workspace title="Settings" description="Update your cloud-saved workspace settings and security." action="Save settings"><SettingsCard /></Workspace>
  return <Workspace title="Performance snapshot" description="Your decisions, measured with context." action="Full analysis →"><MetricGrid metrics={userMetrics} /><div className="content-grid"><ChartCard /><ReviewCard /></div><DataTable rows={['NVDA · +$240 · Plan followed', 'EUR/USD · −$85 · Moved stop', 'BTC/USDT · +$216 · Calm']} /></Workspace>
}

function MarketDashboard() {
  return <Workspace title="Market" description="Review instruments and prices before linking a trade." action="Refresh prices"><div className="content-grid"><Card title="Watchlist"><div className="watchlist"><MarketRow symbol="NVDA" price="$118.42" change="+2.4%" /><MarketRow symbol="EUR/USD" price="1.1684" change="−0.3%" negative /><MarketRow symbol="BTC/USDT" price="$68,420" change="+1.1%" /><MarketRow symbol="ESM6" price="6,124.25" change="+0.6%" /></div></Card><MarketQuoteCard /></div><BarChart title="Market coverage" rows={['Stocks · 4,280 instruments', 'Crypto · 2,100 instruments', 'Forex · 180 pairs', 'Futures · 96 contracts']} /></Workspace>
}

function MarketQuoteCard() {
  const [symbol, setSymbol] = useState('NVDA')
  const [message, setMessage] = useState('Manual mode · configure the Worker API for provider quotes.')
  const [loading, setLoading] = useState(false)
  async function lookup(event: React.FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const result = await getQuote(symbol)
      setMessage(result.price === null ? 'No provider configured; enter prices manually.' : `Provider quote received for ${result.symbol}.`)
    } catch {
      setMessage('Quote service is not connected in this environment.')
    } finally {
      setLoading(false)
    }
  }
  return <Card title="Quote lookup"><form className="quote-form" onSubmit={lookup}><label>Instrument<input value={symbol} onChange={(event) => setSymbol(event.target.value.toUpperCase())} aria-label="Market symbol" /></label><button className="secondary-button" disabled={loading}>{loading ? 'Loading…' : 'Lookup'}</button></form><p className="quote-status">{message}</p></Card>
}

function MarketRow({ symbol, price, change, negative = false }: { symbol: string; price: string; change: string; negative?: boolean }) {
  return <div className="market-row"><span className="symbol">{symbol.slice(0, 2)}</span><strong>{symbol}</strong><span>{price}</span><b className={negative ? 'negative' : 'positive'}>{change}</b></div>
}

function TradeForm({ accessToken, onClose, onSaved }: { accessToken?: string; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<TradeInput>({ symbol: '', market: 'stocks', side: 'long', quantity: 1, entryPrice: 0, exitPrice: undefined, stopLoss: undefined, takeProfit: undefined, fees: 0, leverage: 1, strategy: '', notes: '', psychology: '' })
  const [status, setStatus] = useState('')
  const preview = previewTrade(form)
  const setField = <K extends keyof TradeInput>(key: K, value: TradeInput[K]) => setForm((current) => ({ ...current, [key]: value }))
  const numberField = (key: 'quantity' | 'entryPrice' | 'exitPrice' | 'stopLoss' | 'takeProfit' | 'fees' | 'leverage') => (event: React.ChangeEvent<HTMLInputElement>) => setField(key, event.target.value === '' ? undefined : Number(event.target.value))

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!form.symbol.trim() || form.entryPrice <= 0 || form.quantity <= 0) {
      setStatus('Enter a symbol, quantity, and entry price to continue.')
      return
    }
    if (!accessToken) {
      setStatus('Preview complete. Sign in at /login to save this trade to the encrypted R2 vault.')
      return
    }
    try {
      await saveTrade(form, accessToken)
      onSaved()
    } catch (error) {
      setStatus(error instanceof ApiError ? error.message : 'Unable to save this trade.')
    }
  }

  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="trade-modal" role="dialog" aria-modal="true" aria-labelledby="trade-form-title"><div className="modal-heading"><div><span className="eyebrow">Cloud journal</span><h2 id="trade-form-title">Add trade</h2></div><button className="icon-button" onClick={onClose} aria-label="Close trade form">×</button></div><form onSubmit={submit}><div className="form-grid"><label>Symbol<input value={form.symbol} onChange={(event) => setField('symbol', event.target.value)} placeholder="NVDA" /></label><label>Market<select value={form.market} onChange={(event) => setField('market', event.target.value as TradeMarket)}><option value="stocks">Stocks</option><option value="crypto">Crypto</option><option value="forex">Forex</option><option value="futures">Futures</option><option value="options">Options</option></select></label><label>Direction<select value={form.side} onChange={(event) => setField('side', event.target.value as TradeSide)}><option value="long">Long</option><option value="short">Short</option></select></label><label>Quantity<input type="number" min="0" step="any" value={form.quantity || ''} onChange={numberField('quantity')} /></label><label>Entry price<input type="number" min="0" step="any" value={form.entryPrice || ''} onChange={numberField('entryPrice')} /></label><label>Exit price<input type="number" min="0" step="any" value={form.exitPrice ?? ''} onChange={numberField('exitPrice')} /></label><label>Stop loss<input type="number" min="0" step="any" value={form.stopLoss ?? ''} onChange={numberField('stopLoss')} /></label><label>Take profit<input type="number" min="0" step="any" value={form.takeProfit ?? ''} onChange={numberField('takeProfit')} /></label><label>Leverage<input type="number" min="0.01" step="any" value={form.leverage ?? ''} onChange={numberField('leverage')} /></label></div><div className="trade-preview"><span>Live calculation</span><strong className={preview.pnl === undefined ? '' : preview.pnl >= 0 ? 'positive' : 'negative'}>{preview.pnl === undefined ? 'Open trade' : `${preview.pnl >= 0 ? '+' : ''}$${preview.pnl.toFixed(2)}`}</strong><small>ROI {preview.roiPercent === undefined ? '—' : `${preview.roiPercent >= 0 ? '+' : ''}${preview.roiPercent.toFixed(2)}%`} · Risk {preview.risk === undefined ? '—' : `$${preview.risk.toFixed(2)}`} · Reward {preview.reward === undefined ? '—' : `$${preview.reward.toFixed(2)}`} · R {preview.rMultiple === undefined ? '—' : preview.rMultiple.toFixed(2)}</small></div><label className="wide-field">Notes<textarea value={form.notes} onChange={(event) => setField('notes', event.target.value)} placeholder="Why did you take this trade?" rows={3} /></label>{status && <p className="form-status">{status}</p>}<div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancel</button><button type="submit" className="primary-button">Validate trade</button></div></form></section></div>
}

function AdminDashboard({ view }: { view: AdminView }) {
  if (view === 'users') return <Workspace title="Users" description="Manage account metadata without exposing private journals." action="Invite admin"><DataTable rows={['Alex Kim · Premium · Active', 'Maya Singh · Free · Active', 'Jordan Lee · Trial · Review']} /></Workspace>
  if (view === 'features') return <Workspace title="Features and plans" description="Control free, premium, billing, and rollout entitlements." action="＋ Add feature"><FeatureTable /></Workspace>
  if (view === 'content') return <Workspace title="Content CMS" description="Manage help, education, legal, onboarding, announcements, and translations." action="＋ New content"><DataTable rows={['Getting started guide · Published · 8 languages', 'Options education · Draft · Needs review', 'Privacy policy v3 · Published · Consent active']} /></Workspace>
  if (view === 'integrations') return <Workspace title="Providers" description="Manage market data, brokers, email, payments, AI, calendars, and storage." action="＋ Add provider"><DataTable rows={['Cloudflare R2 · Primary vault · Healthy', 'Market data · Manual fallback · No key configured', 'Transactional email · Healthy · Test delivery']} /></Workspace>
  if (view === 'billing') return <Workspace title="Billing" description="Manage plans, invoices, refunds, retries, taxes, coupons, and webhooks." action="＋ Create plan"><MetricGrid metrics={[{ label: 'MRR', value: '$18,240', detail: '+9.2%', tone: 'positive' }, { label: 'Premium users', value: '214', detail: '8.7% conversion', tone: 'blue' }, { label: 'Failed payments', value: '7', detail: 'Retry queue', tone: 'warning' }, { label: 'Refunds', value: '$420', detail: 'This month', tone: 'warning' }]} /></Workspace>
  if (view === 'operations') return <Workspace title="Operations" description="Monitor services, storage, monetization, and incidents." action="Open status page"><BarChart title="Service health" rows={['Cloudflare Workers · Operational', 'Cloudflare R2 · Operational', 'Supabase Auth · Operational', 'Platform services · Operational']} /></Workspace>
  if (view === 'audit') return <Workspace title="Audit & security" description="Review security events, admin changes, consent, support access, and retention." action="Export audit log"><DataTable rows={['Role changed · Billing admin added · 2 minutes ago', 'Feature changed · AI paused · 1 hour ago', 'Support access · Expired · Yesterday']} /></Workspace>
  return <Workspace title="Admin overview" description="Aggregate operations for JournalEdge services." action="Export report"><MetricGrid metrics={adminMetrics} /><div className="content-grid"><Card title="Service health"><Activity title="Cloudflare R2 vaults" detail="Operational · 99.98% availability" /><Activity title="Subscription operations" detail="Billing webhooks healthy · 96% event coverage" /><Activity title="Auth and roles" detail="Operational · no open security incident" /></Card><Card title="Privacy boundary"><div className="privacy-large"><strong>Private content is excluded.</strong><span>Admin dashboards receive aggregate metrics, billing metadata, service events, and audit records—not private trades, notes, or psychology.</span></div></Card></div></Workspace>
}

function Workspace({ title, description, action, onAction, children }: { title: string; description: string; action: string; onAction?: () => void; children: React.ReactNode }) {
  return <section className="workspace"><div className="workspace-heading"><div><span className="eyebrow">Dashboard</span><h2>{title}</h2><p>{description}</p></div><button className="secondary-button" onClick={onAction}>{action}</button></div>{children}</section>
}

function MetricGrid({ metrics }: { metrics: ReadonlyArray<{ label: string; value: string; detail: string; tone: string }> }) {
  return <div className="metric-grid">{metrics.map(metric => <article className="metric-card" key={metric.label}><span>{metric.label}</span><strong className={metric.tone}>{metric.value}</strong><small>{metric.detail}</small></article>)}</div>
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return <article className="card"><div className="card-heading"><h3>{title}</h3><button className="more" aria-label={`More ${title}`}>•••</button></div>{children}</article>
}

function ChartCard() {
  return <Card title="Equity curve"><div className="chart"><div className="chart-line" /><div className="chart-axis"><span>Mar 1</span><span>Mar 15</span><span>Mar 31</span></div></div></Card>
}

function ReviewCard() {
  return <Card title="Monthly review"><div className="review"><div className="callout"><strong>Your review is ready</strong><span>4 of 6 sections complete. One behavior to improve is waiting.</span></div><div className="progress"><i style={{ width: '72%' }} /></div><small>72% complete</small></div></Card>
}

function DataTable({ rows }: { rows: string[] }) {
  return <Card title="Recent activity"><div className="data-table">{rows.map((row, index) => <div className="data-row" key={row}><span className="symbol">{index + 1}</span><strong>{row}</strong><span className={row.includes('−') ? 'negative' : 'positive'}>{row.includes('−') ? 'Review' : 'View'}</span></div>)}</div></Card>
}

function BarChart({ title, rows }: { title: string; rows: string[] }) {
  return <Card title={title}><div className="bar-list">{rows.map((row, index) => <div className="bar" key={row}><div><strong>{row}</strong><span>{[82, 64, 48, 36][index] ?? 28}%</span></div><i style={{ width: `${[82, 64, 48, 36][index] ?? 28}%` }} /></div>)}</div></Card>
}

function Activity({ title, detail }: { title: string; detail: string }) {
  return <div className="activity"><span className="activity-icon">✓</span><span><strong>{title}</strong><small>{detail}</small></span></div>
}

function SettingsCard() {
  return <div className="settings-list"><div><span><strong>Display name</strong><small>Alex Kim · saved in Supabase</small></span><button className="select">Edit</button></div><div><span><strong>Cloud journal</strong><small>Encrypted R2 storage · active</small></span><span className="status-dot">●</span></div><div><span><strong>Daily review reminder</strong><small>Server-scheduled notification</small></span><button className="toggle on">●</button></div><div><span><strong>Cloud AI consent</strong><small>Off until explicitly enabled</small></span><button className="toggle">○</button></div></div>
}

function FeatureTable() {
  return <div className="data-table"><div className="data-row"><strong>Options strategy builder</strong><span className="tag">Premium</span><button className="select">Edit</button></div><div className="data-row"><strong>Advanced ROI analytics</strong><span className="tag green">All plans</span><button className="select">Edit</button></div><div className="data-row"><strong>Cloud R2 journal</strong><span className="tag green">All plans</span><button className="select">Edit</button></div><div className="data-row"><strong>Cloud AI insights</strong><span className="tag">Premium</span><button className="select">Edit</button></div></div>
}

function selectView(area: Area, key: string, setUserView: (view: UserView) => void, setAdminView: (view: AdminView) => void) {
  if (area === 'admin') setAdminView(key as AdminView)
  else setUserView(key as UserView)
}

function getAreaFromPath(): Area {
  const params = new URLSearchParams(window.location.search)
  if (params.get('area') === 'admin' || window.location.pathname.startsWith('/admin')) return 'admin'
  return 'user'
}

function getRoute() {
  const params = new URLSearchParams(window.location.search)
  return params.get('route') ?? (window.location.pathname.startsWith('/login') ? 'login' : '')
}

export default App
