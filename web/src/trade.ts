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
  leverage?: number
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
  leverage: number
  currency: string
  strategy: string
  notes: string
  psychology: string
  openedAt: string
  status: TradeStatus
  pnl?: number
  risk?: number
  reward?: number
  deployedCapital?: number
  roiPercent?: number
  rMultiple?: number
  createdAt: string
  updatedAt: string
}

export function previewTrade(input: Pick<TradeInput, 'side' | 'quantity' | 'entryPrice' | 'exitPrice' | 'stopLoss' | 'takeProfit' | 'fees' | 'leverage'>) {
  const fees = input.fees ?? 0
  const leverage = input.leverage ?? 1
  const move = input.exitPrice === undefined ? undefined : (input.side === 'long' ? input.exitPrice - input.entryPrice : input.entryPrice - input.exitPrice)
  const pnl = move === undefined ? undefined : move * input.quantity - fees
  const risk = input.stopLoss === undefined ? undefined : Math.abs(input.entryPrice - input.stopLoss) * input.quantity
  const reward = input.takeProfit === undefined ? undefined : Math.abs(input.takeProfit - input.entryPrice) * input.quantity
  const deployedCapital = input.entryPrice > 0 ? input.entryPrice * input.quantity / leverage : undefined
  const roiPercent = pnl === undefined || !deployedCapital ? undefined : pnl / deployedCapital * 100
  return { pnl, risk, reward, deployedCapital, roiPercent, rMultiple: pnl !== undefined && risk ? pnl / risk : undefined }
}
