export const userMetrics = [
  { label: 'Net P&L', value: '+$1,245.80', detail: '8.4% vs last month', tone: 'positive' },
  { label: 'Win rate', value: '58.3%', detail: '42 trades this month', tone: 'blue' },
  { label: 'Expectancy', value: '+$29.66', detail: '+0.42R average', tone: 'positive' },
  { label: 'Max drawdown', value: '−3.2%', detail: 'Within 5% limit', tone: 'warning' },
] as const

export const communityStats = [
  { label: 'Public posts', value: '12.8k', detail: '+18% this month', tone: 'blue' },
  { label: 'Active members', value: '4,820', detail: 'Across 36 groups', tone: 'positive' },
  { label: 'Education views', value: '84.2k', detail: 'Public content only', tone: 'blue' },
  { label: 'Ad revenue', value: '$2,480', detail: 'Contextual placements', tone: 'positive' },
] as const

export const adminMetrics = [
  { label: 'Active users', value: '2,480', detail: '+12.4% this month', tone: 'positive' },
  { label: 'Cloud vault health', value: '99.98%', detail: 'R2 storage operational', tone: 'positive' },
  { label: 'Premium conversion', value: '8.7%', detail: '+1.2 points', tone: 'blue' },
  { label: 'Open incidents', value: '2', detail: 'One review required', tone: 'warning' },
] as const
