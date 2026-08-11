import { useMemo, useState } from 'react'
import { adminMetrics, communityStats, userMetrics } from './dashboardData'

type Area = 'user' | 'community' | 'admin'
type UserView = 'overview' | 'journal' | 'analysis' | 'accounts' | 'settings'
type CommunityView = 'overview' | 'discover' | 'groups' | 'members' | 'guidelines'
type AdminView = 'overview' | 'users' | 'features' | 'operations'

const userNav: Array<[UserView, string, string]> = [
  ['overview', 'Overview', '⌂'],
  ['journal', 'Journal', '▤'],
  ['analysis', 'Analysis', '◔'],
  ['accounts', 'Accounts', '◎'],
  ['settings', 'Settings', '⚙'],
]

const communityNav: Array<[CommunityView, string, string]> = [
  ['overview', 'Overview', '⌂'],
  ['discover', 'Discover', '◉'],
  ['groups', 'Groups', '♧'],
  ['members', 'Members', '♙'],
  ['guidelines', 'Guidelines', '✓'],
]

const adminNav: Array<[AdminView, string, string]> = [
  ['overview', 'Overview', '⌂'],
  ['users', 'Users', '♙'],
  ['features', 'Features', '◆'],
  ['operations', 'Operations', '▥'],
]

function App() {
  const initialArea = getAreaFromPath()
  const [area, setArea] = useState<Area>(initialArea)
  const [userView, setUserView] = useState<UserView>('overview')
  const [communityView, setCommunityView] = useState<CommunityView>('overview')
  const [adminView, setAdminView] = useState<AdminView>('overview')
  const [dark, setDark] = useState(false)

  const title = useMemo(() => {
    if (area === 'admin') return 'Admin control center'
    if (area === 'community') return 'Community workspace'
    return 'Good morning, Alex'
  }, [area])

  function changeArea(next: Area) {
    setArea(next)
    if (next === 'admin') setAdminView('overview')
    if (next === 'community') setCommunityView('overview')
    if (next === 'user') setUserView('overview')
  }

  return (
    <div className={dark ? 'app dark' : 'app'}>
      <aside className="sidebar">
        <Brand />
        <div className="area-switcher" aria-label="Application area">
          <button className={area === 'user' ? 'area active' : 'area'} onClick={() => changeArea('user')}>User app</button>
          <button className={area === 'community' ? 'area active' : 'area'} onClick={() => changeArea('community')}>Community</button>
          {area === 'admin' && <button className="area active" onClick={() => changeArea('admin')}>Admin</button>}
        </div>
        <nav className="nav" aria-label="Primary navigation">
          <span className="nav-label">{area === 'admin' ? 'CONTROL CENTER' : area === 'community' ? 'COMMUNITY' : 'WORKSPACE'}</span>
          {(area === 'admin' ? adminNav : area === 'community' ? communityNav : userNav).map(([key, label, icon]) => {
            const selected = area === 'admin' ? adminView === key : area === 'community' ? communityView === key : userView === key
            return <button key={key} className={selected ? 'nav-item active' : 'nav-item'} onClick={() => selectView(area, key, setUserView, setCommunityView, setAdminView)}><span>{icon}</span>{label}</button>
          })}
          {area !== 'admin' && <>
            <span className="nav-label improve">IMPROVE</span>
            <button className="nav-item" onClick={() => setUserView('journal')}><span>✎</span>Trading diary</button>
          </>}
        </nav>
        <div className="sidebar-bottom">
          {area === 'community' && <div className="ad-note"><strong>Community monetization</strong><span>Public contextual ads only. Private areas stay ad-free.</span></div>}
          <div className="privacy-note"><strong>Cloud-first & encrypted</strong><span>Saved data is protected by Worker API and R2 vaults.</span></div>
          <div className="account-chip"><span className="avatar">AK</span><span><strong>Alex Kim</strong><small>{area === 'admin' ? 'Super admin' : 'Premium plan'}</small></span></div>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="heading"><span className="eyebrow">JournalEdge SaaS</span><h1>{title}</h1></div>
          <div className="top-actions"><span className="domain-pill">{area === 'admin' ? 'admin.' : area === 'community' ? 'community.' : 'app.'}learngermanwith.fun</span><button className="icon-button" aria-label="Toggle theme" onClick={() => setDark(!dark)}>{dark ? '☀' : '◐'}</button><button className="icon-button" aria-label="Notifications">♢</button><button className="primary-button" onClick={() => setUserView('journal')}>＋ Add trade</button></div>
        </header>

        {area === 'admin' ? <AdminDashboard view={adminView} /> : area === 'community' ? <CommunityDashboard view={communityView} /> : <UserDashboard view={userView} />}
      </main>
    </div>
  )
}

function Brand() {
  return <div className="brand"><div className="brand-logo">↗</div><div><strong>JournalEdge</strong><span>Journal every trade. Sharpen your edge.</span></div></div>
}

function UserDashboard({ view }: { view: UserView }) {
  if (view === 'journal') return <Workspace title="Journal" description="Record every decision with cloud-saved context." action="＋ New trade"><DataTable rows={['NVDA · Long · Breakout', 'EUR/USD · Short · Reversal', 'BTC/USDT · Long · Momentum', 'ESM6 · Short · Failed break']} /></Workspace>
  if (view === 'analysis') return <Workspace title="Analysis" description="Turn cloud-saved records into an honest review." action="March 2026"><BarChart title="Strategy performance" rows={['Breakout · +$890 · 66%', 'Reversal · +$210 · 42%', 'Momentum · +$145 · 50%', 'Support · +$108 · 60%']} /></Workspace>
  if (view === 'accounts') return <Workspace title="Accounts" description="Manage live, paper, demo, and prop-firm accounts." action="＋ Connect account"><DataTable rows={['Main account · Interactive Brokers · Live', 'Crypto account · Binance · Live', 'Practice account · Manual · Paper']} /></Workspace>
  if (view === 'settings') return <Workspace title="Settings" description="Update your cloud-saved workspace settings and security." action="Save settings"><SettingsCard /></Workspace>
  return <Workspace title="Performance snapshot" description="Your decisions, measured with context." action="Full analysis →"><MetricGrid metrics={userMetrics} /><div className="content-grid"><ChartCard /><ReviewCard /></div><DataTable rows={['NVDA · +$240 · Plan followed', 'EUR/USD · −$85 · Moved stop', 'BTC/USDT · +$216 · Calm']} /></Workspace>
}

function CommunityDashboard({ view }: { view: CommunityView }) {
  if (view === 'discover') return <Workspace title="Discover" description="Explore public strategies, education, and conversations." action="Search community"><BarChart title="Trending public topics" rows={['Risk management · 2.4k views', 'Options playbooks · 1.8k views', 'Trading psychology · 1.2k views', 'Market review · 940 views']} /></Workspace>
  if (view === 'groups') return <Workspace title="Groups" description="Find private, public, and mentor-led spaces." action="＋ Create group"><DataTable rows={['Risk discipline challenge · Public · 184 members', 'Options Lab · Private · 42 members', 'Coach workspace · Invite-only · 8 members']} /></Workspace>
  if (view === 'members') return <Workspace title="Members" description="Connect with traders while keeping sharing permission-based." action="Invite a mentor"><DataTable rows={['Avery Chen · Coach · Public profile', 'Maya Singh · Options trader · Public profile', 'Jordan Lee · Member · Follows you']} /></Workspace>
  if (view === 'guidelines') return <Workspace title="Guidelines" description="A focused, respectful community for better process—not financial advice." action="Read full policy"><Card title="Community safety"><div className="privacy-large"><strong>Private by default.</strong><span>Public posts are indexable only when the author chooses. Trades, P&L, psychology, messages, and private groups remain uncrawlable and ad-free.</span></div></Card></Workspace>
  return <Workspace title="Community" description="Learn, share, and sharpen your edge with other traders." action="＋ New public post"><MetricGrid metrics={communityStats} /><div className="content-grid"><Card title="Public community activity"><div className="activity-list"><Activity title="Options playbook" detail="Public education · 2.4k views" /><Activity title="Risk discipline challenge" detail="Public group · 184 participants" /><Activity title="Market psychology AMA" detail="Sponsored education · clearly labeled" /></div></Card><Card title="Community monetization"><div className="revenue"><strong>$2,480</strong><span>Public contextual ad revenue this month</span><div className="progress"><i style={{ width: '68%' }} /></div><small>68% of monthly target · private spaces remain ad-free</small></div></Card></div><DataTable rows={['Public strategy library · Indexed by Google', 'Trading psychology discussion · Public', 'Private mentor workspace · Members only']} /></Workspace>
}

function AdminDashboard({ view }: { view: AdminView }) {
  if (view === 'users') return <Workspace title="Users" description="Manage account metadata without exposing private journals." action="Invite admin"><DataTable rows={['Alex Kim · Premium · Active', 'Maya Singh · Free · Active', 'Jordan Lee · Trial · Review']} /></Workspace>
  if (view === 'features') return <Workspace title="Features and plans" description="Control free, premium, community, ads, and rollout entitlements." action="＋ Add feature"><FeatureTable /></Workspace>
  if (view === 'operations') return <Workspace title="Operations" description="Monitor services, storage, monetization, and incidents." action="Open status page"><BarChart title="Service health" rows={['Cloudflare Workers · Operational', 'Cloudflare R2 · Operational', 'Supabase Auth · Operational', 'Community ads · Consent healthy']} /></Workspace>
  return <Workspace title="Admin overview" description="Aggregate operations for JournalEdge services." action="Export report"><MetricGrid metrics={adminMetrics} /><div className="content-grid"><Card title="Service health"><Activity title="Cloudflare R2 vaults" detail="Operational · 99.98% availability" /><Activity title="Community monetization" detail="Contextual ads only · 96% consent coverage" /><Activity title="Auth and roles" detail="Operational · no open security incident" /></Card><Card title="Privacy boundary"><div className="privacy-large"><strong>Private content is excluded.</strong><span>Admin dashboards receive aggregate metrics, billing metadata, service events, and audit records—not private trades, notes, or psychology.</span></div></Card></div></Workspace>
}

function Workspace({ title, description, action, children }: { title: string; description: string; action: string; children: React.ReactNode }) {
  return <section className="workspace"><div className="workspace-heading"><div><span className="eyebrow">Dashboard</span><h2>{title}</h2><p>{description}</p></div><button className="secondary-button">{action}</button></div>{children}</section>
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
  return <div className="data-table"><div className="data-row"><strong>Options strategy builder</strong><span className="tag">Premium</span><button className="select">Edit</button></div><div className="data-row"><strong>Public community ads</strong><span className="tag green">Free</span><button className="select">Edit</button></div><div className="data-row"><strong>Ad-free community</strong><span className="tag">Premium</span><button className="select">Edit</button></div><div className="data-row"><strong>Cloud R2 journal</strong><span className="tag green">All plans</span><button className="select">Edit</button></div></div>
}

function selectView(area: Area, key: string, setUserView: (view: UserView) => void, setCommunityView: (view: CommunityView) => void, setAdminView: (view: AdminView) => void) {
  if (area === 'admin') setAdminView(key as AdminView)
  else if (area === 'community') setCommunityView(key as CommunityView)
  else setUserView(key as UserView)
}

function getAreaFromPath(): Area {
  if (window.location.pathname.startsWith('/admin')) return 'admin'
  if (window.location.pathname.startsWith('/community')) return 'community'
  return 'user'
}

export default App
