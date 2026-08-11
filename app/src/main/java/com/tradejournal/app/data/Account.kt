package com.tradejournal.app.data

import java.util.UUID

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val broker: String,
    val balance: Double,
    val equity: Double,
    val currency: String = "USD",
)
