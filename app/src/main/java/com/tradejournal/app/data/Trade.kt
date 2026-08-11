package com.tradejournal.app.data

import java.util.UUID

data class Trade(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val market: String,
    val direction: String,
    val setup: String,
    val result: Double,
    val rMultiple: Double,
    val status: String,
    val note: String = "",
    val entryPrice: Double = 0.0,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val exitPrice: Double = 0.0,
    val quantity: Double = 1.0,
    val leverage: Double = 1.0,
    val fees: Double = 0.0,
    val sourceFingerprint: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
