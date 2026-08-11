package com.tradejournal.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TradeRepository(private val store: TradeStore) {
    suspend fun loadTrades(): List<Trade> = withContext(Dispatchers.IO) { store.readAll() }

    suspend fun addTrade(trade: Trade) = withContext(Dispatchers.IO) { store.insert(trade) }

    suspend fun seedIfEmpty(seed: List<Trade>) = withContext(Dispatchers.IO) {
        if (store.count() == 0) seed.forEach(store::insert)
    }

    suspend fun loadAccounts(): List<Account> = withContext(Dispatchers.IO) { store.readAccounts() }

    suspend fun seedAccountsIfEmpty(seed: List<Account>) = withContext(Dispatchers.IO) {
        if (store.readAccounts().isEmpty()) seed.forEach(store::insertAccount)
    }

    suspend fun loadDiaryNotes(): List<DiaryNote> = withContext(Dispatchers.IO) { store.readDiaryNotes() }

    suspend fun seedDiaryNotesIfEmpty(seed: List<DiaryNote>) = withContext(Dispatchers.IO) {
        if (store.readDiaryNotes().isEmpty()) seed.forEach(store::insertDiaryNote)
    }

    suspend fun addDiaryNote(note: DiaryNote) = withContext(Dispatchers.IO) { store.insertDiaryNote(note) }
}
