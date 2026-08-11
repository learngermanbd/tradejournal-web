package com.tradejournal.app.data

import kotlin.math.abs

object TradeCalculations {
    data class Preview(
        val grossPnl: Double = 0.0,
        val netPnl: Double = 0.0,
        val stopRisk: Double = 0.0,
        val takeProfitPnl: Double = 0.0,
        val margin: Double = 0.0,
        val rMultiple: Double = 0.0,
    )

    fun preview(
        direction: String,
        entry: Double,
        stopLoss: Double,
        takeProfit: Double,
        exit: Double,
        quantity: Double,
        leverage: Double,
        fees: Double,
    ): Preview {
        val safeQuantity = quantity.coerceAtLeast(0.0)
        val safeLeverage = leverage.coerceAtLeast(1.0)
        val grossPnl = if (entry > 0 && exit > 0) {
            (if (direction == "Short") entry - exit else exit - entry) * safeQuantity
        } else 0.0
        val takeProfitPnl = if (entry > 0 && takeProfit > 0) {
            (if (direction == "Short") entry - takeProfit else takeProfit - entry) * safeQuantity - fees
        } else 0.0
        val stopRisk = if (entry > 0 && stopLoss > 0) {
            abs(if (direction == "Short") entry - stopLoss else stopLoss - entry) * safeQuantity + fees
        } else fees
        val margin = if (entry > 0) entry * safeQuantity / safeLeverage else 0.0
        val netPnl = grossPnl - fees
        return Preview(
            grossPnl = grossPnl,
            netPnl = netPnl,
            stopRisk = stopRisk,
            takeProfitPnl = takeProfitPnl,
            margin = margin,
            rMultiple = if (stopRisk > 0) netPnl / stopRisk else 0.0,
        )
    }
}
