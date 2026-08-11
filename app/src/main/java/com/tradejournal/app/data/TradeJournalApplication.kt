package com.tradejournal.app.data

import android.app.Application

class TradeJournalApplication : Application() {
    val tradeStore by lazy { TradeStore(this) }
    val tradeRepository by lazy { TradeRepository(tradeStore) }
}
