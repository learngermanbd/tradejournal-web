export type TradeSide = 'long' | 'short'
export type TradeMarket = 'stocks' | 'etf' | 'forex' | 'crypto' | 'futures' | 'options' | 'commodities' | 'indices' | 'custom'
export type TradeStatus = 'open' | 'closed' | 'cancelled'

export interface TradeInput {
  symbol: string
  market: TradeMarket
  side: TradeSide
  quantity: number
  entryPrice: number
  exitPrice?: number
  stopLoss?: number
  takeProfit?: number
  fees?: number
  currency?: string
  strategy?: string
  notes?: string
  psychology?: string
  openedAt?: string
  closedAt?: string
  status?: TradeStatus
}

export interface Trade extends TradeInput {
  id: string
  fees: number
  currency: string
  strategy: string
  notes: string
  psychology: string
  openedAt: string
  status: TradeStatus
  pnl?: number
  risk?: number
  reward?: number
  rMultiple?: number
  createdAt: string
  updatedAt: string
}

export function previewTrade(input: Pick<TradeInput, 'side' | 'quantity' | 'entryPrice' | 'exitPrice' | 'stopLoss' | 'takeProfit' | 'fees'>) {
  const fees = input.fees ?? 0
  const move = input.exitPrice === undefined ? undefined : (input.side === 'long' ? input.exitPrice - input.entryPrice : input.entryPrice - input.exitPrice)
  const pnl = move === undefined ? undefined : move * input.quantity - fees
  const risk = input.stopLoss === undefined ? undefined : Math.abs(input.entryPrice - input.stopLoss) * input.quantity
  const reward = input.takeProfit === undefined ? undefined : Math.abs(input.takeProfit - input.entryPrice) * input.quantity
  return { pnl, risk, reward, rMultiple: pnl !== undefined && risk ? pnl / risk : undefined }
}
