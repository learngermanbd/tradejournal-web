export type TradeSide = 'long' | 'short'
export type TradeStatus = 'open' | 'closed' | 'cancelled'
export type TradeMarket = 'stocks' | 'etf' | 'forex' | 'crypto' | 'futures' | 'options' | 'commodities' | 'indices' | 'custom'

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

export interface Trade extends Required<Pick<TradeInput, 'symbol' | 'market' | 'side' | 'quantity' | 'entryPrice'>> {
  id: string
  exitPrice?: number
  stopLoss?: number
  takeProfit?: number
  fees: number
  currency: string
  strategy: string
  notes: string
  psychology: string
  openedAt: string
  closedAt?: string
  status: TradeStatus
  pnl?: number
  risk?: number
  reward?: number
  rMultiple?: number
  createdAt: string
  updatedAt: string
}

export interface ValidationResult {
  valid: boolean
  errors: Record<string, string>
}

const positive = (value: unknown): value is number => typeof value === 'number' && Number.isFinite(value) && value > 0

export function validateTrade(input: TradeInput): ValidationResult {
  const errors: Record<string, string> = {}
  if (!input.symbol?.trim()) errors.symbol = 'Symbol is required.'
  if (!positive(input.quantity)) errors.quantity = 'Quantity must be greater than zero.'
  if (!positive(input.entryPrice)) errors.entryPrice = 'Entry price must be greater than zero.'
  if (input.exitPrice !== undefined && !positive(input.exitPrice)) errors.exitPrice = 'Exit price must be greater than zero.'
  if (input.stopLoss !== undefined && !positive(input.stopLoss)) errors.stopLoss = 'Stop loss must be greater than zero.'
  if (input.takeProfit !== undefined && !positive(input.takeProfit)) errors.takeProfit = 'Take profit must be greater than zero.'
  if (input.fees !== undefined && (!Number.isFinite(input.fees) || input.fees < 0)) errors.fees = 'Fees cannot be negative.'
  if (input.side === 'long' && input.stopLoss !== undefined && input.stopLoss >= input.entryPrice) errors.stopLoss = 'A long stop loss must be below entry.'
  if (input.side === 'short' && input.stopLoss !== undefined && input.stopLoss <= input.entryPrice) errors.stopLoss = 'A short stop loss must be above entry.'
  return { valid: Object.keys(errors).length === 0, errors }
}

export function calculateTrade(input: Pick<TradeInput, 'side' | 'quantity' | 'entryPrice' | 'exitPrice' | 'stopLoss' | 'takeProfit' | 'fees'>) {
  const fees = input.fees ?? 0
  const move = input.exitPrice === undefined ? undefined : (input.side === 'long' ? input.exitPrice - input.entryPrice : input.entryPrice - input.exitPrice)
  const pnl = move === undefined ? undefined : round(move * input.quantity - fees)
  const risk = input.stopLoss === undefined ? undefined : round(Math.abs(input.entryPrice - input.stopLoss) * input.quantity)
  const reward = input.takeProfit === undefined ? undefined : round(Math.abs(input.takeProfit - input.entryPrice) * input.quantity)
  return { pnl, risk, reward, rMultiple: pnl !== undefined && risk && risk > 0 ? round(pnl / risk, 4) : undefined }
}

export function createTrade(input: TradeInput, id = crypto.randomUUID(), now = new Date().toISOString()): Trade {
  const validation = validateTrade(input)
  if (!validation.valid) throw new Error(Object.values(validation.errors).join(' '))
  const calculations = calculateTrade(input)
  return {
    id,
    symbol: input.symbol.trim().toUpperCase(),
    market: input.market,
    side: input.side,
    quantity: input.quantity,
    entryPrice: input.entryPrice,
    exitPrice: input.exitPrice,
    stopLoss: input.stopLoss,
    takeProfit: input.takeProfit,
    fees: input.fees ?? 0,
    currency: input.currency ?? 'USD',
    strategy: input.strategy?.trim() ?? '',
    notes: input.notes?.trim() ?? '',
    psychology: input.psychology?.trim() ?? '',
    openedAt: input.openedAt ?? now,
    closedAt: input.closedAt,
    status: input.status ?? (input.exitPrice === undefined ? 'open' : 'closed'),
    ...calculations,
    createdAt: now,
    updatedAt: now,
  }
}

function round(value: number, decimals = 2) {
  const factor = 10 ** decimals
  return Math.round(value * factor) / factor
}
