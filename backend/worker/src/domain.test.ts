import { describe, expect, it } from 'vitest'
import { calculateTrade, createTrade, validateTrade } from './domain'

describe('trade calculations', () => {
  it('calculates a long trade P&L, risk, reward, and R multiple', () => {
    const result = calculateTrade({ side: 'long', quantity: 10, entryPrice: 100, exitPrice: 115, stopLoss: 95, takeProfit: 120, fees: 5 })
    expect(result).toEqual({ pnl: 145, risk: 50, reward: 200, rMultiple: 2.9 })
  })

  it('calculates a short loss after fees', () => {
    const result = calculateTrade({ side: 'short', quantity: 2, entryPrice: 100, exitPrice: 108, fees: 4 })
    expect(result.pnl).toBe(-20)
  })

  it('rejects invalid directional stops', () => {
    expect(validateTrade({ symbol: 'AAPL', market: 'stocks', side: 'long', quantity: 1, entryPrice: 100, stopLoss: 110 }).valid).toBe(false)
    expect(validateTrade({ symbol: 'AAPL', market: 'stocks', side: 'short', quantity: 1, entryPrice: 100, stopLoss: 90 }).valid).toBe(false)
  })

  it('normalizes and creates an open trade when no exit exists', () => {
    const trade = createTrade({ symbol: ' nvda ', market: 'stocks', side: 'long', quantity: 2, entryPrice: 100 }, '00000000-0000-4000-8000-000000000001', '2026-08-11T00:00:00.000Z')
    expect(trade.symbol).toBe('NVDA')
    expect(trade.status).toBe('open')
    expect(trade.pnl).toBeUndefined()
  })
})
