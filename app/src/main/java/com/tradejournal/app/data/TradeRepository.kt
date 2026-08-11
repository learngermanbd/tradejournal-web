package com.tradejournal.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TradeRepository(private val store: TradeStore) {
    suspend fun loadTrades(): List<Trade> = withContext(Dispatchers.IO) { store.readAll() }

    suspend fun addTrade(trade: Trade) = withContext(Dispatchers.IO) { store.insert(trade) }

    suspend fun seedIfEmpty(seed: List<Trade>) = withContext(Dispatchers.IO) {
        if (store.count() == 0) seed.forEach(store::insert)
    }
}
