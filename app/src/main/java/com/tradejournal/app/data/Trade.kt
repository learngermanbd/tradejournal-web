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
    val createdAt: Long = System.currentTimeMillis(),
)
